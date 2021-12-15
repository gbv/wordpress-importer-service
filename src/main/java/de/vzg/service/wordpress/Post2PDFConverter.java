/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vzg.service.wordpress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import de.vzg.service.wordpress.model.MayAuthorList;
import de.vzg.service.wordpress.model.PostContent;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.MimeConstants;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlgraphics.io.Resource;
import org.apache.xmlgraphics.io.ResourceResolver;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.xml.sax.helpers.DefaultHandler;

import de.vzg.service.wordpress.model.Author;
import de.vzg.service.wordpress.model.Post;

public class Post2PDFConverter {

    private static final Logger LOGGER = LogManager.getLogger();

    private FopFactory fopFactory;

    public Post2PDFConverter() throws URISyntaxException {
        initFopFactory();

    }

    public void getPDF(Post post, OutputStream os, String blog, String license)
        throws FOPException, TransformerException, IOException {
        String htmlContent = getXHtml(post, blog, license);

        ByteArrayOutputStream result;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("cleanup-html.xsl")) {
            Transformer transformer = SAXTransformerFactory.newInstance().newTransformer(new StreamSource(is));
            final byte[] bytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                StreamSource htmlSource = new StreamSource(inputStream);
                result = new ByteArrayOutputStream();
                transformer.transform(htmlSource, new StreamResult(result));
            }
        }
        byte[] cleanBytes = result.toByteArray();
        LOGGER.info(new String(cleanBytes, Charset.defaultCharset()));

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("xhtml2fo.xsl")) {
            Transformer transformer = SAXTransformerFactory.newInstance().newTransformer(new StreamSource(is));
            try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(cleanBytes)) {
                StreamSource htmlSource = new StreamSource(inputStream);
                final FOUserAgent userAgent = fopFactory.newFOUserAgent();
                userAgent.setProducer("Wordpress-Importer-Service");
                DefaultHandler defaultHandler = fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, os)
                    .getDefaultHandler();
                final Result res = new SAXResult(defaultHandler);
                transformer.transform(htmlSource, res);
            }
        }
    }

    private String getXHtml(Post post, String blog, String license) throws IOException {
        String htmlString = getBaseHTML(post, blog);

        htmlString += Optional.ofNullable(post.getContent())
            .map(PostContent::getRendered)
            .orElse(Optional.ofNullable(post.getLayout_flexible_0_text_area())
                .map(s -> "<p>\n</p>" + replaceLangBBCode(s))
                .map(s -> s.replace("\n", "<br />"))
                .map(s -> s.replace("<strong>", "<h3>").replace("</strong>", "</h3>"))
                .orElse("<html></html>"))
            + getLicense(license);

        final Document document = Jsoup.parse(htmlString);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);

        return "<?xml version=\"1.0\"?> \n"
            + document.outerHtml().replace("<html>", "<html xmlns=\"http://www.w3.org/1999/xhtml\">");
    }

    private String replaceLangBBCode(String s) {
        return s.replaceAll("\\[:[a-zA-Z]?[a-zA-Z]?\\]", "");
    }

    private String getLicense(String license) {
        if (license != null) {
            return "<hr/><a href='https://creativecommons.org/licenses/" + license
                + "'><img border='0' src='https://i.creativecommons.org/l/" + license
                + "/80x15.png'></img></a>";
        }
        return "";
    }

    private String getBaseHTML(Post post, String blog) throws IOException {
        String htmlString = "<h1>" + post.getTitle().getRendered() + "</h1>";

        if (post.getWps_subtitle() != null && !post.getWps_subtitle().isEmpty()) {
            htmlString += "<h2>" + post.getWps_subtitle() + "</h2>";
        }

        if (post.getSubline() != null && !post.getSubline().isEmpty()) {
            htmlString += "<h2>" + replaceLangBBCode(post.getSubline()) + "</h2>";
        }

        final List<Integer> authors = Optional.ofNullable(post.getAuthors())
            .orElse(new MayAuthorList())
            .getAuthorIds();

        final String name = authors != null && authors.size() > 0 ? authors.stream().map(authorID -> {
            try {
                return AuthorFetcher.fetchAuthor(blog, authorID);
            } catch (IOException e) {
                throw new RuntimeException("Error while fetching Author " + authorID, e);
            }
        }).map(Author::getName)
            .collect(Collectors.joining(", ")) : UserFetcher.fetchUser(blog, post.getAuthor()).getName();

        htmlString += "<hr/><table border='0'><tr><td>" + name + "</td>";
        htmlString += "<td align='right'>" + post.getDate() + "</td></tr></table>";



        return htmlString;
    }

    private void initFopFactory() throws URISyntaxException {
        fopFactory = new FopFactoryBuilder(new File(".").toURI(), new ResourceResolver() {
            @Override
            public Resource getResource(URI uri) throws IOException {
                try {
                    final URL url = uri.toURL();
                    return new Resource(Request.Get(url.toString())
                        .execute().returnContent().asStream());
                } catch (Throwable t) {
                    LOGGER.error("Error", t);
                    throw t;
                }
            }

            @Override
            public OutputStream getOutputStream(URI uri) throws IOException {
                return uri.toURL().openConnection().getOutputStream();
            }
        }).build();
    }

}
