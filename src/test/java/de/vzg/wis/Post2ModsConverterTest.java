package de.vzg.wis;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import de.vzg.wis.configuration.ImporterConfigurationLicense;
import de.vzg.wis.wordpress.PostFetcher;
import de.vzg.wis.wordpress.model.Post;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.google.gson.Gson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Post2ModsConverterTest {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final ImporterConfigurationLicense CC_MARK_LICENSE = new ImporterConfigurationLicense("https://licensebuttons.net/l/publicdomain/80x15.png", "https://creativecommons.org/publicdomain/mark/1.0/", "cc_mark_1.0");

    @Autowired
    private PostFetcher postFetcher;

    @Test
    public void getMods() throws IOException {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("test-post.json")){
            try(InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)){
                final Post post = new Gson().fromJson(isr, Post.class);
                final Post2ModsConverter converter = new Post2ModsConverter(post, "parent_id_00000001",
                    "https://verfassungsblog.de/", null, CC_MARK_LICENSE);
                final String s = new XMLOutputter(Format.getPrettyFormat()).outputString(converter.getMods());
                LOGGER.info(s);
            }
        }

        try(InputStream is = getClass().getClassLoader().getResourceAsStream("test-post2.json")){
            try(InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)){
                final Post post = postFetcher.getGson().fromJson(isr, Post.class);
                final Post2ModsConverter converter = new Post2ModsConverter(post, "parent_id_00000001",
                    "https://voelkerrechtsblog.org/", null, CC_MARK_LICENSE);
                final String s = new XMLOutputter(Format.getPrettyFormat()).outputString(converter.getMods());
                LOGGER.info(s);
            }
        }
    }
}
