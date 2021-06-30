package de.vzg.service;

import de.vzg.service.configuration.ImporterConfiguration;
import de.vzg.service.configuration.ImporterConfigurationPart;
import de.vzg.service.mycore.AuthApiResponse;
import de.vzg.service.mycore.LocalMyCoReObjectStore;
import de.vzg.service.mycore.MCRObjectIngester;
import de.vzg.service.wordpress.LocalPostStore;
import de.vzg.service.wordpress.Post2PDFConverter;
import de.vzg.service.wordpress.model.Post;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

@WebListener
public class WordpressAutoImporter implements Runnable, ServletContextListener {

    private static final Logger LOGGER = LogManager.getLogger();

    private static ScheduledThreadPoolExecutor EXECUTOR;

    @Override
    public void run() {
        final Map<String, ImporterConfigurationPart> configurationPartMap = ImporterConfiguration.getConfiguration()
                .getParts();
        final Set<String> configs = configurationPartMap.keySet();

        try {
            final List<String> autoConfigurations = configs.stream()
                .filter(configName -> configurationPartMap.get(configName).isAuto()).collect(Collectors.toList());

            for (final String configurationName : autoConfigurations) {
                final ImporterConfigurationPart config = configurationPartMap.get(configurationName);
                LOGGER.info("running import for configuration {}", configurationName);

                LocalMyCoReObjectStore.getInstance(config.getRepository()).update(true);
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
                        final AuthApiResponse authApiResponse = MCRObjectIngester
                            .login(config.getRepository(), config.getUsername(), config.getPassword());

                        loginToken = authApiResponse.getToken_type() + " " + authApiResponse.getAccess_token();
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
                        config.getPostTemplate())
                        .getMods();

                    final ByteArrayOutputStream pdfDocumentStream = new ByteArrayOutputStream();
                    try {
                        new Post2PDFConverter()
                            .getPDF(post, pdfDocumentStream, config.getBlog(), config.getLicense());
                    } catch (FOPException | IOException | URISyntaxException | TransformerException e) {
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

                    LocalMyCoReObjectStore.getInstance(config.getRepository()).update(true);
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

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        LOGGER.info("Starting Auto-Import!");
        EXECUTOR = new ScheduledThreadPoolExecutor(1);
        EXECUTOR.scheduleAtFixedRate(this, 1, 12*60, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        EXECUTOR.shutdownNow();
    }

}
