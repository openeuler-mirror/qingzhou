package qingzhou.command.launcher;

import qingzhou.command.CommandLineProcessor;
import qingzhou.command.CommandUtil;
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
        String instance = args.length > 0 ? args[0] : "instance1";
        if (initInstanceFailed(instance)) return;

        ConfigTool configTool = new ConfigTool(CommandUtil.getInstance());

        // prepare javaCmd
        String javaCmd = CommandUtil.getJavaCmd(configTool.getJavaHomeEnv());
        // 构造启动命令
        StringBuilder startCmd = new StringBuilder(javaCmd);
        for (String arg : configTool.command()) {
            startCmd.append(" ").append(arg);
        }

        // 通过标准输出传递给 standalone 脚本
        System.out.print(startCmd);
    }
}
