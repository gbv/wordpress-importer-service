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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.vzg.wis.Post2ModsConverter;
import de.vzg.wis.Utils;
import de.vzg.wis.configuration.ImporterConfigurationLicense;
import de.vzg.wis.wordpress.model.FailSafeAuthorsDeserializer;
import de.vzg.wis.wordpress.model.MayAuthorList;
import de.vzg.wis.wordpress.model.Post;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.stereotype.Service;


@Service
public class PostFetcher {

    public static final String V2_POSTS_PAGE_PARAM = "page";

    private static final String V2_POSTS_PATH = "wp-json/wp/v2/posts/";


    public static final String V2_POSTS_PER_PAGE = "per_page";

    public static final String V2_POST_COUNT = "X-WP-TotalPages";

    private static final Logger LOGGER = LogManager.getLogger();


    public int fetchCount(String instanceURL) throws IOException {
        final HttpClient httpClient = HttpClientBuilder.create().build();
        final String uri = Utils.getFixedURL(instanceURL) + getEndpoint() + "?" + V2_POSTS_PER_PAGE + "=100";
        LOGGER.debug("Fetching post count from {}", uri);
        final HttpGet get = new HttpGet(uri);
        final HttpResponse execute = httpClient.execute(get);
        return Integer.parseInt(execute.getFirstHeader(V2_POST_COUNT).getValue());
    }

    private String getEndpoint() {
        return V2_POSTS_PATH;
    }

    public List<Post> fetch(String instanceURL) throws IOException {
        LOGGER.debug("Fetching all posts from {}", instanceURL);
        final int count = fetchCount(instanceURL);
        ArrayList<Post> allPosts = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            allPosts.addAll(fetch(instanceURL, i));
        }
        return allPosts;
    }

    public Set<Post> fetchUntil(String instanceURL, OffsetDateTime until) throws IOException {
        final HttpClient httpClient = HttpClientBuilder.create().build();
        int pageCount = 999;
        OffsetDateTime lastChanged = null;
        Set<Post> postsUntil = new HashSet<Post>();
        for (int i = 1; i <= pageCount && (lastChanged == null || lastChanged.isAfter(until)); i++) {
            LOGGER.info("Last changed: {}", lastChanged);

            final String uri = buildURLForPage(instanceURL, i);
            LOGGER.info("Fetching : {}", uri);

            final HttpGet get = new HttpGet(uri);
            final HttpResponse execute = httpClient.execute(get);
            pageCount = Integer.parseInt(execute.getFirstHeader(V2_POST_COUNT).getValue());
            try (final InputStream is = execute.getEntity().getContent()) {
                try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    final Post[] posts = getGson().fromJson(isr, Post[].class);
                    for (Post modifiedPost : posts) {
                        // LOGGER.info("Fetching: {}", modifiedPost.getTitle().getRendered());
                        OffsetDateTime lm = Utils.getWPDate(modifiedPost.getModified());
                        OffsetDateTime published = Utils.getWPDate(modifiedPost.getDate());
                        OffsetDateTime lastChangedIntern = lm.isAfter(published) ? lm : published;

                        if (lastChangedIntern.isAfter(until)) {
                            postsUntil.add(modifiedPost);
                        } else {
                            /*LOGGER.info("Post({}) is old: {} {}>={}", modifiedPost.getId(),
                                modifiedPost.getTitle().getRendered(),
                                lastChangedIntern, until);*/
                        }

                        if (lastChanged == null || lastChangedIntern.isBefore(lastChanged)) {
                            lastChanged = lastChangedIntern;
                        }
                    }
                }
            }
        }
        return postsUntil;
    }

    public Gson getGson() {
        return new GsonBuilder()
                .registerTypeAdapter(MayAuthorList.class, new FailSafeAuthorsDeserializer())
                .create();
    }

    public List<Post> fetch(String instanceURL, int page) throws IOException {
        final HttpClient httpClient = HttpClientBuilder.create().build();
        final String uri = buildURLForPage(instanceURL, page);
        LOGGER.debug("Fetching : {}", uri);
        final HttpGet get = new HttpGet(uri);
        final HttpResponse execute = httpClient.execute(get);

        try (final InputStream is = execute.getEntity().getContent()) {
            try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return Arrays.asList(getGson().fromJson(isr, Post[].class));
            }
        }
    }
    private String buildURLForPage(String instanceURL, int page) {
        return Utils.getFixedURL(instanceURL) + getEndpoint() + "?" + V2_POSTS_PAGE_PARAM + "=" + page + "&"
            + V2_POSTS_PER_PAGE + "=100" + "&orderby" + "=modified";
    }

    public Post fetchPost(String instanceURL, int id) throws IOException {
        final HttpClient httpClient = HttpClientBuilder.create().build();
        final String uri = Utils.getFixedURL(instanceURL) + getEndpoint() + id;
        LOGGER.debug("Fetching : {}", uri);
        final HttpGet get = new HttpGet(uri);
        final HttpResponse execute = httpClient.execute(get);

        try (final InputStream is = execute.getEntity().getContent()) {
            try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return getGson().fromJson(isr, Post.class);
            }
        }
    }

}
