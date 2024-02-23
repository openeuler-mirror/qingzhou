package qingzhou.remote.impl.net.bio;

import qingzhou.remote.impl.net.Channel;
import qingzhou.remote.impl.net.Codec;
import qingzhou.remote.impl.net.Connector;
import qingzhou.remote.impl.net.Handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class BIOConnector implements Connector {
    private Socket socket;
    private int timeout = 5000;
    private Handler handler;
    private Codec codec;
    private InetSocketAddress remoteAddress;

    public BIOConnector(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void setCodec(Codec codec) {
        this.codec = codec;
    }

    @Override
    public Channel connect() throws IOException {
        socket = new Socket();
        socket.connect(remoteAddress, timeout);
        BIOChannel channel = new BIOChannel(socket, handler);
        channel.setCodec(codec);
        return channel;
    }

    @Override
    public void disconnect(int delaySeconds) {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
