package qingzhou.remote.server;

import qingzhou.remote.impl.net.bio.BIOServer;
import qingzhou.serializer.impl.java.JavaSerializer;

public class ServerTest {


    //    @Test todo：会引起编译阻塞
    public void test() {
        JavaSerializer javaSerializer = new JavaSerializer();
        ServerProtocolCodec codec = new ServerProtocolCodec(javaSerializer);

        BIOServer server = new BIOServer(9999);
        server.setCodec(codec);
        server.setHandler(new ServerHandler());
        server.start();
        try {
            Thread.sleep(1000 * 60 * 10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
