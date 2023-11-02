package de.vzg.wis.mycore;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.vzg.wis.UtcDateTypeAdapter;
import de.vzg.wis.Utils;
import de.vzg.wis.configuration.ImporterConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



public class LocalMyCoReObjectStore {

    public static final int FIFTEEN_MINUTES = 1000 * 60 * 15;
    private static final Logger LOGGER = LogManager.getLogger();
    private static SimpleDateFormat SDF_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    private String repoUrl;
    private Map<String, Document> idXMLMap;

    private Date lastCheckDate;

    private LocalMyCoReObjectStore() {
    }

    private LocalMyCoReObjectStore(String repoUrl) {
        this.repoUrl = repoUrl;

        final Path databasePath = getDatabasePath();

        if (Files.exists(databasePath)) {
            loadFromFile();
        } else {
            idXMLMap = new ConcurrentHashMap<>();
            lastCheckDate = new Date(0);
        }
    }

    private synchronized void loadFromFile() {
        Path databasePath = getDatabasePath();
        try (InputStream is = new GZIPInputStream(Files.newInputStream(databasePath))) {
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                final LocalMyCoReObjectStore savedStore = getGson().fromJson(isr, LocalMyCoReObjectStore.class);
                lastCheckDate = savedStore.lastCheckDate;
                repoUrl = savedStore.repoUrl;
                idXMLMap = savedStore.idXMLMap;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while reading " + databasePath.toString());
        }
    }

    public static LocalMyCoReObjectStore getInstance(String url) {
        return InstanceHolder.urlStoreInstanceHolder.computeIfAbsent(url, LocalMyCoReObjectStore::new);
    }

    public Document getObject(String id, String username, String password) {
        this.updateIfNeeded(username, password);
        return idXMLMap.computeIfAbsent(id, (id2) -> this.fetchObject(id2, username, password));
    }

    private Document fetchObject(String id, String username, String password) {
        try {
            LOGGER.debug("Fetching {}", id);
            return ObjectFetcher.fetchObject(repoUrl, username, password, id);
        } catch (IOException | JDOMException | URISyntaxException e) {
            throw new RuntimeException("Could not fetch MODS " + id + "!", e);
        }
    }

    public synchronized void updateIfNeeded(String username, String password) {
        update(false, username, password);
    }

    public synchronized void update(boolean force, String username, String password) {
        if (force || !isUpToDate()) {
            LOGGER.info("Update MyCoRe-Store!");
            final Document lastModifiedDocument = fetchObject("", null, null);
            final Date date = new Date();
            lastModifiedDocument.getRootElement().getChildren("mycoreobject").forEach(mycoreobjectElement -> {
                final String id = mycoreobjectElement.getAttributeValue("ID");
                if (idXMLMap.containsKey(id)) {
                    final Date lastModified;

                    try {
                        lastModified = SDF_UTC.parse(mycoreobjectElement.getAttributeValue("lastModified"));
                    } catch (ParseException e) {
                        throw new RuntimeException("Could not parse lastmodified of:" + id, e);
                    }

                    if (lastModified.getTime() > lastCheckDate.getTime()) {
                        LOGGER.debug("{} needs update {}<{}", id, lastCheckDate.getTime(), lastModified.getTime());
                        this.idXMLMap.put(id, fetchObject(id, username, password));
                    } else {
                        LOGGER.debug("{} needs no update {}>={}", id, lastCheckDate.getTime(), lastModified.getTime());

                    }
                }
            });
            lastCheckDate = date;
        } else {
            LOGGER.debug("No update needed! {}<{}", new Date().getTime() - lastCheckDate.getTime(), FIFTEEN_MINUTES);
        }
    }

    public synchronized void saveToFile() {
        final Gson gson = getGson();

        final Path dbPath = getDatabasePath();
        try (OutputStream os = new GZIPOutputStream(Files
            .newOutputStream(dbPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            try (Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                gson.toJson(this, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while writing mycoredb", e);
        }
    }

    private Gson getGson() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Document.class, new DocumentTypeAdapter());
        gsonBuilder.registerTypeAdapter(Date.class, new UtcDateTypeAdapter());
        return gsonBuilder.create();
    }

    private Path getDatabasePath() {
        return ImporterConfiguration.getConfigPath().resolve(getDatabaseName());
    }

    private String getDatabaseName() {
        return "mycoredb_" + getHost() + ".json";
    }

    private String getHost() {
        String host;
        try {
            host = new URL(this.repoUrl).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return host;
    }

    private boolean isUpToDate() {
        return new Date().getTime() - lastCheckDate.getTime() < FIFTEEN_MINUTES;
    }

    private static class ObjectFetcher {

        private static String token = null;

        private static Instant tokenExpiration = null;

        private static final String V1_OBJECT_PATH = "api/v1/objects/";

        public static Document fetchObject(String repo, String username, String password, String mycoreID)
            throws IOException, JDOMException, URISyntaxException {

            if (username == null || password == null) {
                LOGGER.debug("No username or password given");
            } else if (token == null || tokenExpiration == null
                || tokenExpiration.plus(Duration.of(29, ChronoUnit.MINUTES)).isBefore(Instant.now())) {
                LOGGER.debug("Fetching new token");
                token = MCRRestLogin.getLoginToken(repo, username, password);
                tokenExpiration = Instant.now();
            }

            final HttpClient httpClient = HttpClientBuilder.create().build();
            final String uri = Utils.getFixedURL(repo) + V1_OBJECT_PATH + mycoreID;
            final HttpGet get = new HttpGet(uri);

            get.setHeader("User-Agent", Utils.getUserAgent());
            if (username != null && password != null) {
                get.setHeader("Authorization", token);
            }

            final HttpResponse execute = httpClient.execute(get);

            try (final InputStream is = execute.getEntity().getContent()) {
                SAXBuilder saxBuilder = new SAXBuilder();
                return saxBuilder.build(is);
            }
        }
    }

    private static final class InstanceHolder {
        private static final ConcurrentHashMap<String, LocalMyCoReObjectStore> urlStoreInstanceHolder = new ConcurrentHashMap<>();
    }

}
