package de.vzg.wis.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;


@Entity
@Table(name = "mycore_object_info",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "mycore_id" })
    })
public class MyCoReObjectInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mycore_id", length = 20, nullable = false)
    private String mycoreId;

    @Column(name = "parent_mycore_id", length = 20, nullable = false)
    private String parentMycoreId;

    @Column(name = "wordpress_id", length = 20)
    private Long wordpressId; // saved in recordInfo

    @Column(name = "wordpress_url", length = 1000)
    private String wordpressURL; // saved in recordInfo

    @Column(name = "last_modified")
    private OffsetDateTime lastModified;

    @Column(name = "created")
    private OffsetDateTime created;

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "fulltext", length = 1000)
    private String fulltext; // saved in mods:location/mods:url

    @Column(name = "state", length = 255, nullable = false)
    private String state;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMycoreId() {
        return mycoreId;
    }

    public void setMycoreId(String mycoreId) {
        this.mycoreId = mycoreId;
    }

    public String getParentMycoreId() {
        return parentMycoreId;
    }

    public void setParentMycoreId(String parentMycoreId) {
        this.parentMycoreId = parentMycoreId;
    }

    public Long getWordpressId() {
        return wordpressId;
    }

    public void setWordpressId(Long wordpressId) {
        this.wordpressId = wordpressId;
    }

    public String getWordpressURL() {
        return wordpressURL;
    }

    public void setWordpressURL(String wordpressURL) {
        this.wordpressURL = wordpressURL;
    }

    public OffsetDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(OffsetDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public String getFulltext() {
        return fulltext;
    }

    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

    @Override
    public String toString() {
        return "MyCoReObjectInfo{" +
                "id=" + id +
                ", mycoreId='" + mycoreId + '\'' +
                ", parentMycoreId='" + parentMycoreId + '\'' +
                ", wordpressId=" + wordpressId +
                ", wordpressURL='" + wordpressURL + '\'' +
                ", lastModified=" + lastModified +
                ", created=" + created +
                ", repository='" + repository + '\'' +
                ", fulltext='" + fulltext + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
