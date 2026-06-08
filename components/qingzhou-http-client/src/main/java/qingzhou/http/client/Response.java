package qingzhou.http.client;

public interface Response {
    int getStatus();

    byte[] getBody();
}
