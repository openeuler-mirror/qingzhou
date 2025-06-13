package qingzhou.command.instance;

import java.io.File;
import java.util.List;

import qingzhou.command.CommandUtil;

public enum Cmd {
    start {
        @Override
        public void exec(List<String> extArgs) throws Exception {
            File instance = CommandUtil.getInstance();
            ConfigTool configTool = new ConfigTool(instance);
            // build ProcessBuilder
            ProcessBuilder builder = new ProcessBuilder();
            builder.directory(instance);
            builder.redirectErrorStream(true).inheritIO();
            builder.environment().putAll(configTool.environment());
            builder.command(CommandUtil.getJavaCmd(configTool.getJavaHomeEnv()));
            builder.command().addAll(configTool.command());
            for (String cmd : extArgs) {
                builder.command().add(cmd);
            }

            // 预防命令行执行注入漏洞
            for (String cmd : builder.command()) {
                if (cmd.contains("`")) {
                    throw new IllegalArgumentException("This command may have security risks: " + cmd);
                }
            }

            Process process = builder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }));
            process.waitFor();
        }
    };

    public abstract void exec(List<String> extArgs) throws Exception;
}
