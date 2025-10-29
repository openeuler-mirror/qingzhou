package qingzhou.command.launcher;

import java.io.File;

import qingzhou.command.CommandLineProcessor;
import qingzhou.command.instance.ConfigTool;

public class StartArgs extends CommandLineProcessor {
    @Override
    public String name() {
        return "start-args";
    }

    @Override
    public String info() {
        return "Run the instance. Tip: Put the instance (instance) name at the end of the command.";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        String instance = args.length > 0 ? args[0] : "default";
        File instanceDir = initInstance(instance);
        if (instanceDir == null) return;

        ConfigTool configTool = new ConfigTool(instanceDir, getLibDir());
        // prepare javaCmd
        String javaCmd = configTool.getJavaCmd();
        // 构造启动命令
        StringBuilder startCmd = new StringBuilder(javaCmd);
        for (String arg : configTool.command()) {
            startCmd.append(" ").append(arg);
        }

        // 通过标准输出传递给 standalone 脚本
        System.out.print(startCmd);
    }
}
