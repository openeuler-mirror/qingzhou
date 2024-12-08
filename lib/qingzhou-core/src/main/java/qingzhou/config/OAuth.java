package qingzhou.config;

public class OAuth {
    private boolean enabled = false;
    private String client_id;
    private String client_secret;
    private String authorize_uri;
    private String token_uri;
    private String user_uri;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public void setClient_secret(String client_secret) {
        this.client_secret = client_secret;
    }

    public String getAuthorize_uri() {
        return authorize_uri;
    }

    public void setAuthorize_uri(String authorize_uri) {
        this.authorize_uri = authorize_uri;
    }

    public String getToken_uri() {
        return token_uri;
    }

    public void setToken_uri(String token_uri) {
        this.token_uri = token_uri;
    }

    public String getUser_uri() {
        return user_uri;
    }

    public void setUser_uri(String user_uri) {
        this.user_uri = user_uri;
    }
}
