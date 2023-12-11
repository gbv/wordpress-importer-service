package de.vzg.wis.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface MyCoReObjectInfoRepository extends Repository<MyCoReObjectInfo, Long> {

    MyCoReObjectInfo findFirstByMycoreIdAndRepository(String mycoreId, String repository);

    List<MyCoReObjectInfo> findAllByParentMycoreIdAndRepository(String parentMycoreId, String repository);


    void save(MyCoReObjectInfo myCoReObjectInfo);

    @Query("SELECT m, b FROM MyCoReObjectInfo m, BlogPostInfo b where b.url = m.fulltext " +
            "AND b.blog = ?1 " +
            "AND m.state != 'deleted' " +
            "AND m.state != 'blocked' " +
            "AND m.state != 'hold_back'")
    List<Object[]> findObjectsWithBlog(String blog);



}
