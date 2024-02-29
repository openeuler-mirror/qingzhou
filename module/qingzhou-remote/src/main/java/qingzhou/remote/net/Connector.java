package qingzhou.remote.net;

import java.io.IOException;

public interface Connector {

    void setHandler(Handler handler);

    void setCodec(Codec codec);

    Channel connect() throws IOException;

    void disconnect(int delaySeconds);
}
