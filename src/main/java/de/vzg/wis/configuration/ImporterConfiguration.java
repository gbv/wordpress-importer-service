package de.vzg.wis.configuration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;


public class ImporterConfiguration {

    private Map<String, ImporterConfigurationPart> parts;

    private ImporterConfiguration() {
        this.parts = new HashMap<>();
    }

    public static ImporterConfiguration getConfiguration() {
        return ConfigurationInstanceHolder.instance;
    }

    public Map<String, ImporterConfigurationPart> getParts() {
        return parts;
    }

    private void setParts(Map<String, ImporterConfigurationPart> parts) {
        this.parts = parts;
    }

    public static Path getConfigPath() {
        String wpHome = System.getProperty("wp.home");
        final String homeFolder = System.getProperty("user.home");

        return wpHome == null ? Paths.get(homeFolder).resolve(".wpimport") : Paths.get(wpHome);
    }

    private static class ConfigurationInstanceHolder {
        private static final ImporterConfiguration instance = initConfiguration();

        private static ImporterConfiguration initConfiguration() {
            try {
                final Path wpimportFolder = getConfigPath();
                final Path wpConfigFile = wpimportFolder.resolve("config.json");

                if (!Files.exists(wpimportFolder)) {
                    Files.createDirectories(wpimportFolder);
                }

                if (!Files.exists(wpConfigFile)) {
                    return new ImporterConfiguration();
                }

                try (InputStream is = Files.newInputStream(wpConfigFile)) {
                    try (Reader reader = new InputStreamReader(is)) {
                        GsonBuilder gson = new GsonBuilder();
                        final ImporterConfiguration config = gson.registerTypeAdapter(ImporterConfigurationLicense.class,
                                new BackwardsCompatibleLicenseDeserializer()).create().fromJson(reader, ImporterConfiguration.class);
                        return config;
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Error while reading configuration!", e);
            }
        }
    }
}
