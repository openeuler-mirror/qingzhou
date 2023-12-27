package qingzhou.bootstrap.command.impl.server;

import qingzhou.bootstrap.Utils;

import java.io.File;
import java.util.List;

public enum Cmd {
    start {
        @Override
        public void exec(File domainDir, List<String> extArgs) throws Exception {
            if (ProcessHolder.checkIfStarted(Server.getDomainDir())) {
                Server.log("The server has already started.");
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
    };

    public abstract void exec(File domainDir, List<String> extArgs) throws Exception;
}
