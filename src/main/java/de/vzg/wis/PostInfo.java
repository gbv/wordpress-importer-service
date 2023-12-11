package de.vzg.wis;

public class PostInfo {

    private String title;
    private int id;
    private String url;

    public PostInfo(String title, int id, String url) {
        this.title = title;
        this.id = id;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }
}
