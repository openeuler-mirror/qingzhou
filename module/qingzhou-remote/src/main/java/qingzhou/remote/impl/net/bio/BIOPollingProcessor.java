package qingzhou.remote.impl.net.bio;

import qingzhou.remote.impl.net.Channel;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BIOPollingProcessor {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void add(Channel channel) {
        executor.execute(new Processor(channel));
    }

    private class Processor implements Runnable {
        private final Channel channel;

        public Processor(Channel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Object res = channel.read();
                    channel.write(res);
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        channel.getSocket().close();
                    } catch (IOException ignored) {
                    }
                    break;
                }
            }
        }
    }

}
