package de.vzg.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import de.vzg.service.wordpress.PostFetcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import com.google.gson.Gson;

import de.vzg.service.wordpress.model.Post;

public class Post2ModsConverterTest {

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void getMods() throws IOException {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("test-post.json")){
            try(InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)){
                final Post post = new Gson().fromJson(isr, Post.class);
                final Post2ModsConverter converter = new Post2ModsConverter(post, "parent_id_00000001",
                    "https://verfassungsblog.de/", null);
                final String s = new XMLOutputter(Format.getPrettyFormat()).outputString(converter.getMods());
                LOGGER.info(s);
            }
        }

        try(InputStream is = getClass().getClassLoader().getResourceAsStream("test-post2.json")){
            try(InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)){
                final Post post = PostFetcher.getGson().fromJson(isr, Post.class);
                final Post2ModsConverter converter = new Post2ModsConverter(post, "parent_id_00000001",
                    "https://voelkerrechtsblog.org/", null);
                final String s = new XMLOutputter(Format.getPrettyFormat()).outputString(converter.getMods());
                LOGGER.info(s);
            }
        }
    }
}
