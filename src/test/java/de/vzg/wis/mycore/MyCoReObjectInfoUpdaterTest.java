package de.vzg.wis.mycore;

import com.google.gson.Gson;
import de.vzg.wis.ArticleDetectorService;
import de.vzg.wis.WordpressMyCoReComparingResult;
import de.vzg.wis.jpa.BlogPostInfo;
import de.vzg.wis.wordpress.BlogPostInfoUpdater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MyCoReObjectInfoUpdaterTest {

    @Autowired
    private MyCoReObjectInfoUpdater myCoReObjectInfoUpdater;

    @Autowired
    private BlogPostInfoUpdater blogPostInfoUpdater;

    @Autowired
    private ArticleDetectorService articleDetectorService;

    private static final Logger LOGGER = LogManager.getLogger();

    void testUpdateMyCoReObjectInfo() throws IOException, URISyntaxException, JDOMException {
        /*

        blogPostInfoUpdater.updateBlogPostInfo("https://voelkerrechtsblog.org/");

        WordpressMyCoReComparingResult cr = articleDetectorService.getComparingResult("https://voelkerrechtsblog.org/", "mir_mods_00000733");

        String json = new Gson().toJson(cr);
        LOGGER.info("Result: " + json);

        */

    }
}