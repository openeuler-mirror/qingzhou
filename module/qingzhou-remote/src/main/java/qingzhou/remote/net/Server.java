package qingzhou.remote.net;

public interface Server {

    void setHandler(Handler handler);

    void setCodec(Codec codec);

    void start();

    void stop(int delaySeconds);
}
