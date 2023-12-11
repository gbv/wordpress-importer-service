package de.vzg.wis;

import de.vzg.wis.jpa.BlogPostInfo;
import de.vzg.wis.wordpress.BlogPostInfoUpdater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;


@SpringBootTest
class BlogPostInfoUpdaterTest {

    @Autowired
    private BlogPostInfoUpdater blogPostInfoUpdater;

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String BLOG_URL = "Test URL";

    @Test
    void getNewestPostDate() {
        /*
        OffsetDateTime date = OffsetDateTime.now(ZoneId.systemDefault());
        BlogPostInfo bpInfo1 = new BlogPostInfo();

        bpInfo1.setBlog(BLOG_URL);
        bpInfo1.setTitle("Test Title1");
        bpInfo1.setDate(date);
        bpInfo1.setUrl("/test");
        bpInfo1.setWordpressId(1L);


        OffsetDateTime newerDate = date.plus(1, ChronoUnit.MINUTES);
        BlogPostInfo bpInfo2 = new BlogPostInfo();

        bpInfo2.setBlog(BLOG_URL);
        bpInfo2.setTitle("Test Title3");
        bpInfo2.setDate(newerDate);
        bpInfo2.setUrl("/test2");
        bpInfo2.setWordpressId(2L);

        OffsetDateTime olderDate = date.minus(1, ChronoUnit.MINUTES);
        BlogPostInfo bpInfo3 = new BlogPostInfo();

        bpInfo3.setTitle("Test Title3");
        bpInfo3.setBlog(BLOG_URL);
        bpInfo3.setDate(olderDate);
        bpInfo3.setUrl("/test3");
        bpInfo3.setWordpressId(3L);

        blogPostInfoUpdater.addBlogPostInfo(bpInfo1);
        blogPostInfoUpdater.addBlogPostInfo(bpInfo2);
        blogPostInfoUpdater.addBlogPostInfo(bpInfo3);



        OffsetDateTime newestPostDate = blogPostInfoUpdater.getNewestPostDate(BLOG_URL);

        LOGGER.info("Date: " + date);
        LOGGER.info("Newer Date: " + newerDate);
        LOGGER.info("Newest Post Date by service: " + newestPostDate);

        Assertions.assertEquals(newerDate, newestPostDate, "Newest Post Date is not the newest date");
*/
    }


    @Test
    void updateBlogPostInfo() throws IOException {
        LOGGER.info("Test updateBlogPostInfo");
        //blogPostInfoUpdater.updateBlogPostInfo("https://verfassungsblog.de/");
    }

}