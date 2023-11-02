package de.vzg.wis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import de.vzg.wis.configuration.ImporterConfiguration;
import de.vzg.wis.configuration.ImporterConfigurationPart;
import de.vzg.wis.mycore.LocalMyCoReObjectStore;
import de.vzg.wis.mycore.MCRObjectIngester;
import de.vzg.wis.mycore.MCRRestLogin;
import de.vzg.wis.wordpress.LocalPostStore;
import de.vzg.wis.wordpress.Post2PDFConverter;
import de.vzg.wis.wordpress.model.Post;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;


import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class WordpressAutoImporter implements Runnable, ServletContextListener {

    private static final Logger LOGGER = LogManager.getLogger();

    private static ScheduledThreadPoolExecutor EXECUTOR;

    @Override
    public void run() {
        final Map<String, ImporterConfigurationPart> configurationPartMap = ImporterConfiguration.getConfiguration()
                .getParts();
        final Set<String> configs = configurationPartMap.keySet();

        LOGGER.info("Running autoimporter for configurations: {}", String.join(", ", configs));

        try {
            final List<String> autoConfigurations = configs.stream()
                .filter(configName -> configurationPartMap.get(configName).isAuto()).collect(Collectors.toList());

            for (final String configurationName : autoConfigurations) {
                final ImporterConfigurationPart config = configurationPartMap.get(configurationName);
                LOGGER.info("running import for configuration {}", configurationName);

                LocalMyCoReObjectStore.getInstance(config.getRepository()).update(true, config.getUsername(),
                    config.getPassword());
                final WordpressMyCoReCompare wordpressMyCoReCompare = new WordpressMyCoReCompare(config);

                final WordpressMyCoReComparingResult compare;
                try {
                    compare = wordpressMyCoReCompare.compare();
                } catch (IOException | JDOMException e) {
                    LOGGER.error("Error while comparing posts for configuration: " + configurationName, e);
                    LOGGER.info("Continue with next configuration!");
                    continue;
                }

                final List<PostInfo> notImportedPosts = compare.getNotImportedPosts();

                for (PostInfo postInfo : notImportedPosts) {
                    LOGGER.info("Import the post with id: {}  title: {} and url: {}", postInfo.getId(), postInfo.getTitle(),
                        postInfo.getUrl());

                    String loginToken;
                    try {
                        loginToken = MCRRestLogin.getLoginToken(config.getRepository(), config.getUsername(),
                            config.getPassword());
                    } catch (IOException | URISyntaxException e) {
                        LOGGER.error("Error while login to repository: " + config.getRepository(), e);
                        LOGGER.info("Continue with next configuration!");
                        continue;
                    }

                    final LocalPostStore postStore = LocalPostStore.getInstance(config.getBlog());
                    final Post post = postStore.getPost(postInfo.getId());
                    final Document mods = new Post2ModsConverter(post,
                        config.getParentObject(),
                        config.getBlog(),
                        config.getPostTemplate(),
                        config.getLicense())
                        .getMods();

                    final ByteArrayOutputStream pdfDocumentStream = new ByteArrayOutputStream();
                    try {
                        new Post2PDFConverter()
                            .getPDF(post, pdfDocumentStream, config.getBlog(), config.getLicense());
                    } catch (IOException | URISyntaxException | TransformerException e) {
                        LOGGER.error("Error while generating PDF for post ID: " + post.getId() + " LINK: " + post.getLink(),
                            e);
                        LOGGER.info("Continue with next post!");
                        continue;
                    }

                    final String objectID;
                    try {
                        objectID = MCRObjectIngester.ingestObject(config.getRepository(), loginToken, mods);
                    } catch (IOException e) {
                        final String modsAsString = new XMLOutputter(Format.getPrettyFormat()).outputString(mods);
                        LOGGER.error("Error while ingesting mods: " + modsAsString + " \n to " + config.getRepository(), e);
                        LOGGER.info("Continue with next post!");
                        continue;
                    }
                    final String derivateID;
                    try {

                        derivateID = MCRObjectIngester
                            .createDerivate(config.getRepository(),
                                loginToken,
                                objectID);
                    } catch (IOException e) {
                        LOGGER.error("Error while ingesting Derivate: " + post.getId() + "!", e);
                        LOGGER.info("Continue with next post!");
                        continue;
                    }

                    try {
                        MCRObjectIngester.uploadFile(config.getRepository(),
                            loginToken,
                            derivateID,
                            objectID,
                            pdfDocumentStream.toByteArray(),
                            Utils.getTitleFileName(post));
                    } catch (IOException e) {
                        LOGGER.error("Error while ingesting Derivate: " + post.getId() + "!", e);
                        LOGGER.info("Continue with next post!");
                    }

                    LocalMyCoReObjectStore.getInstance(config.getRepository()).update(true, config.getUsername(),
                        config.getPassword());
                }
            }

        } catch (Throwable e) {
            LOGGER.error("Error while running autoimporter!", e);
        }


        try {
           HashSet<String> repos = new HashSet<>();
           HashSet<String> blogs = new HashSet<>();
            for (final String configurationName : configs) {
                final ImporterConfigurationPart config = configurationPartMap.get(configurationName);
                String repository = config.getRepository();
                String blog = config.getBlog();
                repos.add(repository);
                blogs.add(blog);
            }
            blogs.stream().map(LocalPostStore::getInstance).forEach(LocalPostStore::saveToFile);
            repos.stream().map(LocalMyCoReObjectStore::getInstance).forEach(LocalMyCoReObjectStore::saveToFile);
        } catch (Throwable e) {
            LOGGER.error("Error while storing DBs!", e);

        }
    }


}
