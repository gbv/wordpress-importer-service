package de.vzg.wis.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.Date;

@Entity()
@Table(name = "blog_post_info",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wordpress_id", "blog"}
))
public class BlogPostInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 1000, nullable = false)
    private String title;

    @Column(name = "url", length = 1000, nullable = false)
    private String url;

    @Column(name = "wordpress_id", nullable = false)
    private long wordpressId;

    @Column(name = "blog", length = 255, nullable = false)
    private String blog;

    @Column(name = "date", nullable = false)
    private OffsetDateTime date;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getWordpressId() {
        return wordpressId;
    }

    public void setWordpressId(long wordpressId) {
        this.wordpressId = wordpressId;
    }

    public String getBlog() {
        return blog;
    }

    public void setBlog(String blog) {
        this.blog = blog;
    }


    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "BlogPostInfo{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", wordpressId=" + wordpressId +
                ", blog='" + blog + '\'' +
                ", date=" + date +
                '}';
    }
}
