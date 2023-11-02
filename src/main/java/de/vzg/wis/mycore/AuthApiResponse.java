package de.vzg.wis.mycore;

public class AuthApiResponse {

    public AuthApiResponse() {
    }

    boolean login_success;
    String access_token;
    String token_type;

    public boolean isLogin_success() {
        return login_success;
    }

    public void setLogin_success(boolean login_success) {
        this.login_success = login_success;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public String asAuthString(){
        return this.getToken_type() + " " + getAccess_token();
    }
}
