package qingzhou.http.client;

public interface ResponseListener {
    void onBody(String line);

    void onComplete();

    void onError(Throwable t);
}
