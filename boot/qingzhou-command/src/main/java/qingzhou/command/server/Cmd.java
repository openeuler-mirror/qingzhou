package qingzhou.command.server;

import java.io.File;
import java.util.List;

public enum Cmd {
    start {
        @Override
        public void exec(File instanceDir, List<String> extArgs) throws Exception {
            ProcessHolder processHolder = new ProcessHolder(instanceDir, extArgs);
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
    };

    public abstract void exec(File instanceDir, List<String> extArgs) throws Exception;
}
