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

package de.vzg.wis.wordpress;

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
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import de.vzg.wis.wordpress.model.*;
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

import de.vzg.wis.configuration.ImporterConfigurationLicense;


public class Post2PDFConverter {

    private static final Logger LOGGER = LogManager.getLogger();

    private FopFactory fopFactory;

    public Post2PDFConverter() throws URISyntaxException {
        initFopFactory();

    }

    public void getPDF(Post post, OutputStream os, String blog, ImporterConfigurationLicense license,
        String additionalXHTML)
            throws TransformerException, IOException {
        String htmlContent = getXHtml(post, blog, license, additionalXHTML);

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
            } catch (FOPException e) {
                throw new TransformerException("Error while formatting PDF", e);
            }
        }
    }

    private String getXHtml(Post post, String blog, ImporterConfigurationLicense license, String additionalXHTML)
        throws IOException {
        String htmlString = getBaseHTML(post, blog);

        if (additionalXHTML != null) {
            htmlString = additionalXHTML + htmlString;
        }

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

    private String getLicense(ImporterConfigurationLicense license) {
        if (license != null) {
            if (license.getLogoURL() != null && license.getURL() != null) {
                return "<hr/><a href='" + license.getURL()
                        + "'><img border='0' src='" + license.getLogoURL() + "'></img></a>";
            } else if (license.getLabel() != null && license.getURL() != null) {
                return "<hr/><a href='" + license.getURL() + "'>" + license.getLabel() + "</a>";
            } else if (license.getLabel() != null) {
                return "<hr/><span>" + license.getLabel() + "</span>";
            }
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

        String subtitle = post.getACF("subline");
        if (subtitle != null && !subtitle.isBlank()) {
            htmlString += "<h2>" + subtitle + "</h2>";
        }

        final List<Integer> authorIds = Optional.ofNullable(post.getAuthors())
                .orElse(new MayAuthorList())
                .getAuthorIds();

        final List<String> authorNames = Optional.ofNullable(post.getAuthors())
                .orElse(new MayAuthorList())
                .getAuthorNames();

        String combinedNamesStr;
        if (authorIds != null && authorIds.size() > 0) {
            combinedNamesStr = authorIds.stream().map(authorID -> {
                        try {
                            return AuthorFetcher.fetchAuthor(blog, authorID);
                        } catch (IOException e) {
                            throw new RuntimeException("Error while fetching Author " + authorID, e);
                        }
                    }).map(Author::getName)
                    .collect(Collectors.joining(", "));
        } else if (authorNames != null && authorNames.size()>0){
            combinedNamesStr = String.join(", ", authorNames);
        } else if (post.getDelegate1() != null || post.getDelegate2() != null || post.getDelegate3() != null) {
            List<String> delegateAuthors = Stream.of(post.getDelegate1(), post.getDelegate2(), post.getDelegate3())
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(String::isEmpty))
                    .collect(Collectors.toList());
            combinedNamesStr = String.join(", ", delegateAuthors);
        } else {
            combinedNamesStr = UserFetcher.fetchUser(blog, post.getAuthor()).getName();
        }

        if(post.getCoAuthors() != null && !post.getCoAuthors().isEmpty()) {
            String filteredCoauthors = post.getCoAuthors()
                    .stream()
                    .map(CoAuthor::getDisplay_name)
                    .filter(Predicate.not(combinedNamesStr::contains))
                    .collect(Collectors.joining(", "));
            if (!combinedNamesStr.isEmpty()) {
                combinedNamesStr += ", " + filteredCoauthors;
            }
        }

        htmlString += "<hr/><table border='0'><tr><td>" + combinedNamesStr + "</td>";
        String dateString = post.getDate();
        if (!blog.contains("youthdelegatesearch")) {
            htmlString += "<td align='right'>" + dateString + "</td></tr>";
        } else {
            htmlString += "<td align='right'> </td>";
        }
        htmlString += "</table>";

        if (blog.contains("youthdelegatesearch")) {
            String undoc = "UN Doc. " + post.getAcf().getAsJsonObject().get("undoc").getAsString();
            String pagesStr = post.getAcf().getAsJsonObject().get("pages").getAsString();
            htmlString += "<p><b>UN Youth Delegate Programme</b></p>";
            TemporalAccessor parsedDateTime = DateTimeFormatter.ISO_DATE_TIME.parse(dateString);
            String formattedDate = DateTimeFormatter.ofPattern("d LLLL y").withLocale(Locale.ENGLISH).format(parsedDateTime);
            htmlString += "<p>Original: " + undoc + ", " + formattedDate + ", p. " + pagesStr + "</p>";
            htmlString += "<p>Youth Delegate Search: <a href=\"" + post.getLink() + "\">" + post.getLink() + "</a></p>";
        }

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
