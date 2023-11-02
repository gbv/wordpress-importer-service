package de.vzg.wis;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.vzg.wis.configuration.ImporterConfigurationPart;
import de.vzg.wis.mycore.LocalMyCoReObjectStore;
import de.vzg.wis.mycore.MODSUtil;
import de.vzg.wis.wordpress.LocalPostStore;
import de.vzg.wis.wordpress.model.Post;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;


public class WordpressMyCoReCompare {

    private static final Logger LOGGER = LogManager.getLogger();

    private ImporterConfigurationPart config;

    public WordpressMyCoReCompare(ImporterConfigurationPart config) {
        this.config = config;
    }

    public WordpressMyCoReComparingResult compare() throws IOException, JDOMException {
        final WordpressMyCoReComparingResult comparingResult = new WordpressMyCoReComparingResult();
        final LocalMyCoReObjectStore mcrStore = LocalMyCoReObjectStore.getInstance(config.getRepository());
        final LocalPostStore postStore = LocalPostStore.getInstance(config.getBlog());
        final List<Post> allPosts = postStore.getAllPosts();
        final Map<String, String> mycoreIDValidationMap = comparingResult.getMycoreIDValidationMap();
        final HashMap<String, Post> notImported = new HashMap<>();

        for (Post cur : allPosts) {
            final String curlLink = cur.getLink();
            if (notImported.containsKey(curlLink)) {
                //LOGGER.warn("Double post link: {} and {}", cur.getId(), notImported.get(curlLink).getId());
            }
            notImported.put(curlLink, cur);
        }

        final Document parentMods = mcrStore.getObject(config.getParentObject(), config.getUsername(), config.getPassword());
        final List<String> children = MODSUtil.getChildren(parentMods);

        for (int i = 0; notImported.size() > 0 && i < children.size(); i++) {
            String child = children.get(i);
            final Document childDoc = mcrStore.getObject(child, config.getUsername(), config.getPassword());
            final Optional<String> fulltextURL = MODSUtil.getFulltextURL(childDoc);
            if(!MODSUtil.isLockedOrDeleted(childDoc)) {
                if (fulltextURL.isPresent()) {
                    if (notImported.containsKey(fulltextURL.get())) {
                        final Post post = notImported.remove(fulltextURL.get());
                        comparingResult.getMyCoReIDPostMap().put(child, getInfo(post));
                    }
                } else {
                    mycoreIDValidationMap.put(child, "URL zum Blog ist nicht eingetragen!");
                    LOGGER.warn("{} has no url to a blog post!", child);
                }
            }
        }

        comparingResult.getNotImportedPosts()
            .addAll(notImported.values().stream().sorted(Comparator.comparing(Post::getDate).reversed())
                .map(WordpressMyCoReCompare::getInfo).collect(Collectors
                    .toList()));

        return comparingResult;
    }

    private static PostInfo getInfo(Post post) {
        return new PostInfo(post.getTitle().getRendered(), post.getId(), post.getLink());

    }

}
