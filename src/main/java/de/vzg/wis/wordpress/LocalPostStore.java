package de.vzg.wis.wordpress;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.vzg.wis.configuration.ImporterConfiguration;
import de.vzg.wis.wordpress.model.Post;


public class LocalPostStore {

    private Map<Integer, Post> idPostMap;

    private String instanceURL;

    private Date lastUpdate;

    private boolean isArticleEndpoint;

    private LocalPostStore(String instanceURL) {
        this.instanceURL = instanceURL;

        if (Files.exists(getDatabasePath())) {
            loadFromFile();
        } else {
            this.lastUpdate = new Date(0);
            this.idPostMap = new ConcurrentHashMap<>();
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
            throw new RuntimeException("Error while writing postdb", e);
        }
    }

    private synchronized void loadFromFile() {
        Path databasePath = getDatabasePath();
        try (InputStream is = new GZIPInputStream(Files.newInputStream(databasePath))) {
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                final LocalPostStore savedStore = getGson().fromJson(isr, LocalPostStore.class);
                this.idPostMap = savedStore.idPostMap;
                this.instanceURL = savedStore.instanceURL;
                this.lastUpdate = savedStore.lastUpdate;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while reading " + databasePath.toString());
        }
    }

    public static LocalPostStore getInstance(String instanceURL) {
        return InstanceHolder.blogStoreMap.computeIfAbsent(instanceURL, (instance) -> new LocalPostStore(instanceURL));
    }

    public List<Post> getAllPosts() {
        this.update();
        return new ArrayList<>(this.idPostMap.values());
    }

    private synchronized void update() {
        try {
            final Date currentDate = new Date();
            final Set<Post> posts = PostFetcher.fetchUntil(instanceURL, lastUpdate);
            posts.forEach(p -> this.idPostMap.put(p.getId(), p));

            this.lastUpdate = currentDate;
        } catch (IOException e) {
            throw new RuntimeException("Error while gettings Posts from " + this.instanceURL, e);
        }
    }

    private static class InstanceHolder {
        private static Map<String, LocalPostStore> blogStoreMap = new ConcurrentHashMap<>();
    }

    private Gson getGson() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        return gsonBuilder.create();
    }

    private Path getDatabasePath() {
        return ImporterConfiguration.getConfigPath().resolve(getDatabaseName());
    }

    private String getDatabaseName() {
        return "blogdb_" + getHost() + ".gzip.json";
    }

    public Post getPost(int id){
        this.update();
        return this.idPostMap.get(id);
    }

    private String getHost() {
        String host;
        try {
            host = new URL(this.instanceURL).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return host;
    }

}
