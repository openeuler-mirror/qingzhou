package qingzhou.remote.client;


import org.testng.annotations.Test;
import qingzhou.remote.impl.net.Channel;
import qingzhou.remote.impl.net.Connector;
import qingzhou.remote.impl.net.bio.BIOConnector;
import qingzhou.serializer.impl.java.JavaSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ConnectorTest {

    @Test
    public void test() throws IOException {
        JavaSerializer javaSerializer = new JavaSerializer();
        ClientProtocolCodec codec = new ClientProtocolCodec(javaSerializer);
        Connector connector = new BIOConnector(new InetSocketAddress("localhost", 9999));
        connector.setCodec(codec);
        connector.setHandler(new ClientHandler());
        Channel channel = connector.connect();
        if (channel.isConnected()) {
            int req = 1;
            for (int i = 0; i < 10; i++) {
                Integer res = (Integer) request(channel, req);
                System.out.println("收到响应：" + res);
                req = res + 1;
            }
        }
    }

    private static Object request(Channel channel, Integer i) throws IOException {
        channel.write(i);
        return channel.read();
    }
}
