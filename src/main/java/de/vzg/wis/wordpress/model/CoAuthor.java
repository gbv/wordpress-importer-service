package de.vzg.wis.wordpress.model;

public class CoAuthor {

    private int id;
    private String display_name;
    private String user_nicename;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public CoAuthor() {
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getUser_nicename() {
        return user_nicename;
    }

    public void setUser_nicename(String user_nicename) {
        this.user_nicename = user_nicename;
    }
}
