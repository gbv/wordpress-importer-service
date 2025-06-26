package de.vzg.wis.mycore;

import de.vzg.wis.Utils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ObjectFetcher {

    private static final Logger LOGGER = LogManager.getLogger();

    private static String token = null;

    private static Instant tokenExpiration = null;

    private static final String V1_OBJECT_PATH = "api/v1/objects/";
    private static final String V2_OBJECT_PATH = "api/v2/objects/";

    public Document fetchObject(String repo, String username, String password, String mycoreID)
            throws IOException, JDOMException, URISyntaxException {
        LOGGER.info("Fetching object " + mycoreID);

        if (username == null || password == null) {
            LOGGER.debug("No username or password given");
        } else if (token == null || tokenExpiration == null
                || tokenExpiration.plus(Duration.of(9, ChronoUnit.MINUTES)).isBefore(Instant.now())) {
            LOGGER.debug("Fetching new token");
            token = MCRRestLogin.getLoginToken(repo, username, password);
            tokenExpiration = Instant.now();
        }

        final HttpClient httpClient = HttpClientBuilder.create().build();
        final String uri = Utils.appendSlashIfNotPresent(repo) + V2_OBJECT_PATH + (mycoreID.isBlank() ? "?limit=9999999" : mycoreID);
        final HttpGet get = new HttpGet(uri);

        get.setHeader("User-Agent", Utils.getUserAgent());
        if (username != null && password != null) {
            get.setHeader("Authorization", token);
        }

        final HttpResponse execute = httpClient.execute(get);

        if (execute.getStatusLine().getStatusCode() == 200) {
            try (final InputStream is = execute.getEntity().getContent()) {
                SAXBuilder saxBuilder = new SAXBuilder();
                return saxBuilder.build(is);
            }
        }
        throw new RuntimeException("Could not fetch " + uri + " " + execute.getStatusLine().getStatusCode()
                + " " + execute.getStatusLine().getReasonPhrase());
    }
}
