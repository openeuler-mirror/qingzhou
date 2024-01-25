package qingzhou.remote.impl.net;

import qingzhou.remote.impl.net.Channel;
import qingzhou.remote.impl.net.Codec;
import qingzhou.remote.impl.net.Handler;

import java.io.IOException;

public interface Connector {

    void setHandler(Handler handler);

    void setCodec(Codec codec);

    Channel connect() throws IOException;

    void disconnect(int delaySeconds);
}
