package qingzhou.command.server;

import qingzhou.command.Utils;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public enum Cmd {
    start {
        @Override
        public void exec(File domainDir, List<String> extArgs) throws Exception {
            if (checkIfStarted(domainDir)) {
                Utils.log("The server has already started.");
                return;
            }

            {
                extArgs.add(0, "start");
                ProcessHolder processHolder = new ProcessHolder(Utils.getServerXml(domainDir), extArgs);
                ProcessBuilder processBuilder = processHolder.buildProcess();
                processBuilder.redirectErrorStream(true);
                processBuilder.inheritIO();
                Process process = processHolder.startProcess();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        process.waitFor();
                    } catch (InterruptedException ignored) {
                    }
                }));
                process.waitFor();
            }
        }

        private boolean checkIfStarted(File domain) {
            File pidFile = Utils.getTemp(domain, "server.pid");
            if (!pidFile.exists()) {
                return false;
            }

            // 如果被kill上，上述判断会不准确
            Map<String, String> attributes = Utils.getAttributes(Utils.getServerXml(Utils.getDomain()), "//server/connector");
            int serverPort = Integer.parseInt(attributes.get("port"));
            return isPortOpened(serverPort);
        }

        private boolean isPortOpened(int port) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1000);// 如果超时时间太长，会导致创建域的页面卡顿！！
                s.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    };

    public abstract void exec(File domainDir, List<String> extArgs) throws Exception;
}
