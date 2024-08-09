package de.vzg.wis.configuration;

public class ImporterConfigurationPart {

    /**
     * The url to the wordpress blog
     */
    private String blog;

    /**
     * The url to the mycore repository
     */
    private String repository;

    /**
     * The parent object to which the posts will be appended
     */
    private String parentObject;

    /**
     * Post will be automatic imported every day
     */
    private boolean auto;

    /**
     * The username of the user on which behalf the posts should be imported
     */
    private String username;

    /**
     * The password of the user on which behalf the posts should be imported
     */
    private String password;

    /**
     * The template which will be used to generate a object
     */
    private String postTemplate;

    /**
     * Additional XHTML which will be prepended to the post
     */
    private String additionalXHTML;

    /**
     * Should the PDF be imported (default: true)
     */
    private boolean importPDF = true;

    private ImporterConfigurationLicense license;

    public String getBlog() {
        return blog;
    }

    public void setBlog(String blog) {
        this.blog = blog;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getParentObject() {
        return parentObject;
    }

    public void setParentObject(String parentObject) {
        this.parentObject = parentObject;
    }

    public String getPostTemplate() {
        return postTemplate;
    }

    public void setPostTemplate(String postTemplate) {
        this.postTemplate = postTemplate;
    }

    public ImporterConfigurationLicense getLicense() {
        return license;
    }

    public void setLicense(ImporterConfigurationLicense license) {
        this.license = license;
    }

    public boolean isAuto() {
        return auto;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAdditionalXHTML() {
        return additionalXHTML;
    }

    public void setAdditionalXHTML(String additionalXHTML) {
        this.additionalXHTML = additionalXHTML;
    }

    public boolean isImportPDF() {
        return importPDF;
    }

    public void setImportPDF(boolean importPDF) {
        this.importPDF = importPDF;
    }
}
