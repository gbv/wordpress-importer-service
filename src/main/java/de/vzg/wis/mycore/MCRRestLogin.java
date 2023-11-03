package de.vzg.wis.mycore;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import de.vzg.wis.Utils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;


public class MCRRestLogin {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String AUTH_API_PATH = "api/v1/auth/login";

    public static AuthApiResponse login(String repo, String userName, String password)
        throws IOException, URISyntaxException {
        final String uriString = Utils.appendSlashIfNotPresent(repo) + AUTH_API_PATH;
        URI uri = new URI(uriString);

        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);
        credentialsProvider.setCredentials(new AuthScope(uri.getHost(), uri.getPort()), credentials);

        final HttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider)
            .build();

        final HttpGet get = new HttpGet(uri);
        get.setHeader("User-Agent", Utils.getUserAgent());
        final HttpResponse execute = httpClient.execute(get);

        try (final InputStream is = execute.getEntity().getContent()) {
            try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                final Gson gson = new Gson();
                return gson.fromJson(isr, AuthApiResponse.class);
            }
        }
    }

    public static String getLoginToken(String repository, String username, String password)
        throws IOException, URISyntaxException {
        final AuthApiResponse authApiResponse = login(repository, username, password);
        return authApiResponse.getToken_type() + " " + authApiResponse.getAccess_token();
    }
}
