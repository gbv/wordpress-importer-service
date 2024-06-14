package de.vzg.wis.wordpress;

import de.vzg.wis.Utils;
import de.vzg.wis.jpa.BlogPostInfo;
import de.vzg.wis.jpa.BlogPostInfoRepository;
import de.vzg.wis.wordpress.PostFetcher;
import de.vzg.wis.wordpress.model.Post;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Controller
public class BlogPostInfoUpdater {

    private static final Logger LOGGER = LogManager.getLogger();

    @Autowired
    private BlogPostInfoRepository blogPostInfoRepository;

    @Autowired
    private PostFetcher postFetcher;

    public OffsetDateTime getNewestPostDate(String blogURL) {
        var newestPost = blogPostInfoRepository.findFirstByBlogOrderByDateDesc(blogURL);
        if (newestPost != null) {
            LOGGER.debug("Newest Post is : {} ({}) ", newestPost.getTitle(), newestPost.getDate().toString());
            return newestPost.getDate();
        }
        LOGGER.debug("No newest Post found");
        return null;
    }

    public void updateBlogPostInfo(String blogURL) throws IOException {
        var newestPost = blogPostInfoRepository.findFirstByBlogOrderByDateDesc(blogURL);

        LOGGER.info("newestPost: {}", newestPost);
        OffsetDateTime date = null;

        if (newestPost != null) {
            date = newestPost.getDate();
        } else {
            date = OffsetDateTime.now().minus(30, ChronoUnit.YEARS);
        }

        Set<Post> posts = postFetcher.fetchUntil(blogURL, date);
        posts.forEach(post -> {
            BlogPostInfo existing
                = blogPostInfoRepository.findFirstByWordpressIdAndBlog((long) post.getId(), blogURL);

            if (existing == null) {
                existing = new BlogPostInfo();
                existing.setWordpressId(post.getId());
                existing.setBlog(blogURL);
                LOGGER.info("Adding new post({}): {} to Database", post.getId(), post.getTitle().getRendered());
            } else {
                LOGGER.info("Updating existing post({}) {} in Database", existing.getWordpressId(),
                    existing.getTitle());
            }

            existing.setTitle(post.getTitle().getRendered());
            existing.setDate(Utils.getWPDate(post.getModified()));
            existing.setUrl(post.getLink());
            blogPostInfoRepository.save(existing);
        });
    }

    public void addBlogPostInfo(BlogPostInfo blogPostInfo) {
        blogPostInfoRepository.save(blogPostInfo);
    }
}
