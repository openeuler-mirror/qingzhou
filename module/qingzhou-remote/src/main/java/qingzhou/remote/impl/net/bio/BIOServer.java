package qingzhou.remote.impl.net.bio;

import qingzhou.remote.impl.net.Codec;
import qingzhou.remote.impl.net.Handler;
import qingzhou.remote.impl.net.Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BIOServer implements Server {

    private static final int DEFAULT_PORT = 7000;

    private int port;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final BIOPollingProcessor processor = new BIOPollingProcessor();
    private Handler handler;
    private Codec codec;
    private ServerSocket serverSocket;
    private boolean init;

    public BIOServer() {
        this(DEFAULT_PORT);
    }

    public BIOServer(int port) {
        this.port = port;
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
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            init = true;
            executorService.submit(new Acceptor());
            System.out.println("Server started with port:" + this.port);
        } catch (IOException e) {
            System.out.println("Server start failed！");
            printStackTrace(e);
        }
    }

    @Override
    public void stop(int delaySeconds) {
        try {
            init = false;
            serverSocket.close();
            if (!executorService.isShutdown()) {
                executorService.shutdown();
            }
            System.out.println("Server shutdown success.");
        } catch (IOException e) {
            System.out.println("Server shutdown exception.");
            e.printStackTrace();
        }
    }

    private void printStackTrace(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer, true));
        System.out.println(writer.getBuffer().toString());
    }

    private class Acceptor implements Runnable {
        @Override
        public void run() {
            while (init) {
                try {
                    Socket socket = serverSocket.accept();
                    BIOChannel channel = new BIOChannel(socket, handler);
                    channel.setCodec(codec);
                    processor.add(channel);
                } catch (IOException e) {
                    printStackTrace(e);
                }
            }
        }
    }

}
