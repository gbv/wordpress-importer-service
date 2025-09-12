package de.vzg.wis.wordpress;

import com.google.gson.Gson;
import de.vzg.wis.Utils;
import de.vzg.wis.wordpress.model.CoAuthor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CoAuthorFetcher {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String V1_COAUTHOR_PATH = "wp-json/coauthors/v1/coauthors/";

    public static List<CoAuthor> fetchCoAuthors(String instanceURL, int postId) throws IOException {
        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String uri = Utils.getFixedURL(instanceURL) + V1_COAUTHOR_PATH + "?post_id=" + postId;
            LOGGER.debug("Fetching : {}", uri);
            final HttpGet get = new HttpGet(uri);
            final HttpResponse execute = httpClient.execute(get);

            LOGGER.info("Fetch coauthors of " + instanceURL + " postid " + postId);

            try (final InputStream is = execute.getEntity().getContent()) {
                try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    return new ArrayList<>(List.of(new Gson().fromJson(isr, CoAuthor[].class)));
                }
            }
        }
    }

}
