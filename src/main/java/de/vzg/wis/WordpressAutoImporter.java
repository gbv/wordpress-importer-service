package de.vzg.wis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.vzg.wis.configuration.ImporterConfiguration;
import de.vzg.wis.configuration.ImporterConfigurationPart;
import de.vzg.wis.mycore.MCRObjectIngester;
import de.vzg.wis.mycore.MCRRestLogin;
import de.vzg.wis.mycore.MyCoReObjectInfoUpdater;
import de.vzg.wis.wordpress.BlogPostInfoUpdater;
import de.vzg.wis.wordpress.Post2PDFConverter;
import de.vzg.wis.wordpress.PostFetcher;
import de.vzg.wis.wordpress.model.Post;


@Service
public class WordpressAutoImporter implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger();

    @Autowired
    private ArticleDetectorService articleDetectorService;

    @Autowired
    private BlogPostInfoUpdater postInfoUpdater;

    @Autowired
    private MyCoReObjectInfoUpdater objectInfoUpdater;

    @Autowired
    private PostFetcher postFetcher;

    @Autowired
    private MCRObjectIngester objectIngester;


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


                objectInfoUpdater.updateMyCoReObjectInfo(config.getRepository(), config.getUsername(),
                        config.getPassword(), config.getParentObject());
                postInfoUpdater.updateBlogPostInfo(config.getBlog());

                WordpressMyCoReComparingResult compare;

                try {
                    compare = articleDetectorService.getComparingResult(config.getBlog(), config.getParentObject());
                } catch (Throwable e) {
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

                   Post post = postFetcher.fetchPost(config.getBlog(), postInfo.getId());

                    final Document mods = new Post2ModsConverter(post,
                        config.getParentObject(),
                        config.getBlog(),
                        config.getPostTemplate(),
                        config.getLicense())
                        .getMods();

                    final ByteArrayOutputStream pdfDocumentStream = new ByteArrayOutputStream();
                    if(config.isImportPDF()) {
                        try {
                            new Post2PDFConverter()
                                .getPDF(post, pdfDocumentStream, config.getBlog(), config.getLicense(),
                                        config.getAdditionalXHTML());
                        } catch (IOException | URISyntaxException | TransformerException e) {
                            LOGGER.error("Error while generating PDF for post ID: " + post.getId() + " LINK: " + post.getLink(),
                                e);
                            LOGGER.info("Continue with next post!");
                            continue;
                        }
                    }


                    final String objectID;
                    try {
                        objectID = objectIngester.ingestObject(config.getRepository(), loginToken, mods);
                    } catch (IOException e) {
                        final String modsAsString = new XMLOutputter(Format.getPrettyFormat()).outputString(mods);
                        LOGGER.error("Error while ingesting mods: " + modsAsString + " \n to " + config.getRepository(), e);
                        LOGGER.info("Continue with next post!");
                        continue;
                    }

                    if(config.isImportPDF()) {
                        final String derivateID;
                        try {

                            derivateID = objectIngester.createDerivate(config.getRepository(),
                                    loginToken,
                                    objectID);
                        } catch (IOException e) {
                            LOGGER.error("Error while ingesting Derivate: " + post.getId() + "!", e);
                            LOGGER.info("Continue with next post!");
                            continue;
                        }

                        try {
                            objectIngester.uploadFile(config.getRepository(),
                                    loginToken,
                                    derivateID,
                                    objectID,
                                    pdfDocumentStream.toByteArray(),
                                    Utils.getTitleFileName(post));
                        } catch (IOException e) {
                            LOGGER.error("Error while ingesting Derivate: " + post.getId() + "!", e);
                            LOGGER.info("Continue with next post!");
                        }
                    }

                    objectInfoUpdater.updateMyCoReObject(config.getRepository(), config.getUsername(),
                            config.getPassword(), objectID, null);
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Error while running autoimporter!", e);
        }

    }


}
