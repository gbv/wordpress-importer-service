package de.vzg.wis.configuration;

public class ImporterConfigurationLicense {


    private String logoURL;
    private String URL;
    private String classID;
    private String label;

    public ImporterConfigurationLicense() {
        logoURL = null;
        URL = null;
        classID = null;
        label = null;
    }

    public ImporterConfigurationLicense(String logoURL, String URL, String classID) {
        this();
        this.logoURL = logoURL;
        this.URL = URL;
        this.classID = classID;
    }

    public ImporterConfigurationLicense(String label, String classID) {
        this();
        this.logoURL = null;
        this.URL = null;
        this.label = label;
        this.classID = classID;
    }

    public String getLogoURL() {
        return logoURL;
    }

    public void setLogoURL(String logoURL) {
        this.logoURL = logoURL;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getClassID() {
        return classID;
    }

    public void setClassID(String classID) {
        this.classID = classID;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
