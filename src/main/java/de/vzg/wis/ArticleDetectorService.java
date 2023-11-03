package de.vzg.wis;

import de.vzg.wis.jpa.BlogPostInfo;
import de.vzg.wis.jpa.BlogPostInfoRepository;
import de.vzg.wis.jpa.MyCoReObjectInfo;
import de.vzg.wis.jpa.MyCoReObjectInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class ArticleDetectorService {

    @Autowired
    private BlogPostInfoRepository blogPostInfoRepository;

    @Autowired
    private MyCoReObjectInfoRepository myCoReObjectInfoRepository;


    public List<BlogPostInfo> getNotImportedArticles(String blog, String parent) {
        List<BlogPostInfo> articles = blogPostInfoRepository.findAllArticlesWithoutMyCoReObject(blog, parent);
        return articles;
    }

    public HashMap<MyCoReObjectInfo, BlogPostInfo> getImportedArticles(String blog) {
        List<Object[]> objectsWithBlog = myCoReObjectInfoRepository.findObjectsWithBlog(blog);

        HashMap<MyCoReObjectInfo, BlogPostInfo> objectInfoBlogInfoMap = new HashMap<>();

        for (Object[] objects : objectsWithBlog) {
            MyCoReObjectInfo objectInfo = (MyCoReObjectInfo) objects[0];
            BlogPostInfo blogPostInfo = (BlogPostInfo) objects[1];
            objectInfoBlogInfoMap.put(objectInfo, blogPostInfo);
        }

        return objectInfoBlogInfoMap;
    }

    public WordpressMyCoReComparingResult getComparingResult(String blog, String parent){
        WordpressMyCoReComparingResult result = new WordpressMyCoReComparingResult();

        List<BlogPostInfo> notImportedArticles = getNotImportedArticles(blog, parent);
        notImportedArticles.stream()
                .map(this::convertToPostInfo)
                .forEach(result.getNotImportedPosts()::add);

        HashMap<MyCoReObjectInfo, BlogPostInfo> importedArticles = getImportedArticles(blog);
        importedArticles.forEach((myCoReObjectInfo, blogPostInfo) -> {
            result.getMyCoReIDPostMap().put(myCoReObjectInfo.getMycoreId(), convertToPostInfo(blogPostInfo));
        });

        return result;
    }

    private PostInfo convertToPostInfo(BlogPostInfo article) {
        return new PostInfo(article.getTitle(), (int) article.getWordpressId(), article.getUrl());
    }

}
