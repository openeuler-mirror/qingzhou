package qingzhou.console.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class StreamUtil {
    public static void readInputStreamWithThread(InputStream inputStream, StringCollector output, String name) {
        Thread thread = new Thread(new StreamConsumer(inputStream, name, output), "read-input-stream-with-thread");
        thread.setDaemon(true);
        thread.start();
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }

    private StreamUtil() {
    }

    private static class StreamConsumer implements Runnable {
        InputStream is;
        String type;
        StringCollector output;

        public StreamConsumer(InputStream inputStream, String type, StringCollector output) {
            this.is = inputStream;
            this.type = type;
            this.output = output;
        }

        /**
         * Runs this object as a separate thread, printing the contents of the InputStream
         * supplied during instantiation, to either stdout or stderr
         */
        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (output != null) {
                        String msg = type + ">" + line + System.lineSeparator();
                        output.collect(msg);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (output != null) {
                    String msg = type + ">" + e.getMessage() + System.lineSeparator();
                    output.collect(msg);
                }
            }
        }
    }

}
