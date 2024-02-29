package qingzhou.remote.net;

import java.io.IOException;
import java.net.Socket;

public interface Channel {

    Object read() throws IOException;

    void write(Object message) throws IOException;

    Socket getSocket();

    boolean isConnected();

    Codec getCodec();

    Handler getHandler();

}
