package de.vzg.wis.wordpress;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import de.vzg.wis.Utils;
import de.vzg.wis.wordpress.model.Author;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;



public class AuthorFetcher {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String V2_AUTHOR_PATH = "wp-json/wp/v2/authors/";

    public static Author fetchAuthor(String instanceURL, int id) throws IOException {
        final HttpClient httpClient = HttpClientBuilder.create().build();
        final String uri = Utils.getFixedURL(instanceURL) + V2_AUTHOR_PATH + id;
        LOGGER.debug("Fetching : {}", uri);
        final HttpGet get = new HttpGet(uri);
        final HttpResponse execute = httpClient.execute(get);

        LOGGER.info("Fetch author " + instanceURL + " id " + id);

        try (final InputStream is = execute.getEntity().getContent()) {
            try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return new Gson().fromJson(isr, Author.class);
            }
        }
    }
}
