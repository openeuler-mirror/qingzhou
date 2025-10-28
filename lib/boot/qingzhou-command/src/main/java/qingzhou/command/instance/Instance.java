package qingzhou.command.instance;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import qingzhou.command.CommandLineProcessor;

public class Instance extends CommandLineProcessor {
    private static final String START_NAME = "start";

    public Instance() {
        // 实现 CommandLineProcessor 接口，须有无参的公开构造方法
    }

    @Override
    public String name() {
        return "instance";
    }

    @Override
    public String info() {
        return "Manage Qingzhou service";
    }

    @Override
    public String[] supportedArgs() {
        return new String[]{START_NAME};
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        if (args == null || args.length < 1) {
            log("command line argument pattern: <cmd> [instance] [...]");
            return;
        }

        if (args.length == 1) {
            args = new String[]{args[0], "default"};
        }

        String instance = args[1];
        File instanceDie = initInstance(instance);
        if (instanceDie == null) return;

        String cmdName = args[0];
        if (!START_NAME.equals(cmdName)) {
            log("Command <" + cmdName + "> not found.");
            return;
        }

        String[] extArgs = Arrays.copyOfRange(args, 2, args.length);
        start(instanceDie, getLibDir(), new ArrayList<>(Arrays.asList(extArgs)));
    }

    private void start(File instanceDir, File libDir, List<String> extArgs) throws Exception {
        ConfigTool configTool = new ConfigTool(instanceDir, libDir);
        // build ProcessBuilder
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(instanceDir);
        builder.redirectErrorStream(true).inheritIO();
        builder.environment().putAll(configTool.environment());
        builder.command(configTool.getJavaCmd());
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
            } catch (InterruptedException ignored) {
            }
        }));
        process.waitFor();
    }
}
