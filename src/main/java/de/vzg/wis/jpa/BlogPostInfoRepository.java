package de.vzg.wis.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface BlogPostInfoRepository extends Repository<BlogPostInfo, Long> {


    BlogPostInfo findFirstByBlogOrderByDateDesc(String blog);


    void save(BlogPostInfo blogPostInfo);

    BlogPostInfo findFirstByWordpressIdAndBlog(Long wordpressId, String blog);

    List<BlogPostInfo> findAllArticlesByBlog(String blog);

    @Query("SELECT b FROM BlogPostInfo b WHERE b.blog = ?1 AND b.url NOT IN (SELECT m.fulltext FROM MyCoReObjectInfo m where m.parentMycoreId = ?2 and m.fulltext is not null)")
    List<BlogPostInfo> findAllArticlesWithoutMyCoReObject(String blog, String parent);

}
