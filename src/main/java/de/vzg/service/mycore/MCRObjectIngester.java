package de.vzg.service.mycore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.google.gson.Gson;

import de.vzg.service.Utils;

public class MCRObjectIngester {

    private static final String AUTH_API_PATH = "api/v1/auth/login";

    private static final String OBJECT_API_PATH = "api/v1/objects";

    public static AuthApiResponse login(String repo, String userName, String password)
        throws IOException, URISyntaxException {
        final String uriString = Utils.getFixedURL(repo) + AUTH_API_PATH;
        URI uri = new URI(uriString);

        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);
        credentialsProvider.setCredentials(new AuthScope(uri.getHost(), uri.getPort()), credentials);

        final HttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider)
            .build();

        final HttpGet get = new HttpGet(uri);
        final HttpResponse execute = httpClient.execute(get);

        try (final InputStream is = execute.getEntity().getContent()) {
            try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                final Gson gson = new Gson();
                return gson.fromJson(isr, AuthApiResponse.class);
            }
        }
    }

    public static String ingestObject(String repo, String auth, Document object) throws IOException {
        final String uriString = Utils.getFixedURL(repo) + OBJECT_API_PATH;
        final HttpClient httpClient = HttpClientBuilder.create().build();
        final HttpPost post = new HttpPost(uriString);
        post.setHeader("Authorization", auth);
        final String entity = new XMLOutputter(Format.getPrettyFormat()).outputString(object);

        post.setEntity(MultipartEntityBuilder.create()
            .addBinaryBody("file", new ByteArrayInputStream(entity.getBytes(StandardCharsets.UTF_8))).build());

        final HttpResponse execute = httpClient.execute(post);

        if (execute.getStatusLine().getStatusCode() == 201) {
            final String location = execute.getFirstHeader("Location").getValue();
            return location.substring(location.lastIndexOf("/") + 1);
        }

        throw new IOException(
            "Error while ingesting MCRObject. " + execute.getStatusLine().getStatusCode() + " - " + execute
                .getStatusLine().getReasonPhrase());
    }

    public static String createDerivate(String repo, String auth, String parentObjectID)
        throws IOException {
        final String uriString = Utils.getFixedURL(repo) + OBJECT_API_PATH + "/" + parentObjectID + "/derivates";
        LogManager.getLogger().info("Sending derivate to {}", uriString);
        final HttpClient httpClient = HttpClientBuilder.create().build();
        final HttpPost post = new HttpPost(uriString);
        post.setHeader("Authorization", auth);

        final MultipartEntityBuilder formDataBuilder = MultipartEntityBuilder.create()
            .addTextBody("label", "data object from " + parentObjectID);
        post.setEntity(formDataBuilder.build());

        final HttpResponse execute = httpClient.execute(post);

        if (execute.getStatusLine().getStatusCode() == 201) {
            final String location = execute.getFirstHeader("Location").getValue();

            return location.substring(location.lastIndexOf("/") + 1);
        }

        throw new IOException(
            "Error while ingesting MCRDerivate. " + execute.getStatusLine().getStatusCode() + " - " + execute
                .getStatusLine().getReasonPhrase());
    }

    public static void uploadFile(String repo, String auth, String derivate, String parentObjectID, byte[] pdf,
        String fileName) throws IOException {
        final String uriString =
            Utils.getFixedURL(repo) + OBJECT_API_PATH + "/" + parentObjectID + "/derivates/" + derivate + "/contents";
        final HttpClient httpClient = HttpClientBuilder.create().build();
        final HttpPost post = new HttpPost(uriString);
        post.setHeader("Authorization", auth);
        LogManager.getLogger().info("Sending file to {}", uriString);

        String md5 = DigestUtils
            .md5Hex(pdf).toUpperCase();

        final MultipartEntityBuilder formDataBuilder = MultipartEntityBuilder.create()
            .addBinaryBody("file", pdf)
            .addTextBody("path", "/" + fileName)
            .addTextBody("maindoc", "true")
            .addTextBody("unzip", "false")
            .addTextBody("size", String.valueOf(pdf.length))
            .addTextBody("md5", md5);

        post.setEntity(formDataBuilder.build());

        final HttpResponse execute = httpClient.execute(post);

        if (execute.getStatusLine().getStatusCode() == 201) {
            return;
        }

        throw new IOException(
            "Error while ingesting File. " + execute.getStatusLine().getStatusCode() + " - " + execute
                .getStatusLine().getReasonPhrase());
    }

}
