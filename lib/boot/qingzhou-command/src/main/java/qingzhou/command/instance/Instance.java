package qingzhou.command.instance;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import qingzhou.command.CommandLineProcessor;
import qingzhou.command.CommandUtil;

public class Instance extends CommandLineProcessor {
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
        return new String[]{Cmd.start.name()};
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        if (args == null || args.length < 1) {
            CommandUtil.log("command line argument pattern: <cmd> [instance] [...]");
            return;
        }

        if (args.length == 1) {
            args = new String[]{args[0], "instance1"};
        }

        File homeDir = CommandUtil.getLibDir().getParentFile().getParentFile();

        String instance = args[1];
        File base = new File(instance);
        if (base.isAbsolute()) {
            instance = base.getAbsolutePath();
        } else {
            instance = new File(new File(homeDir, "instances"), instance).getAbsolutePath();
        }

        File instanceDir = new File(instance);
        if (!instanceDir.isDirectory()) {
            CommandUtil.log("Instance does not exist: " + base.getName());
            return;
        }

        String cmdName = args[0];
        String outputInfo = instance;
        if (instance.startsWith(homeDir.getAbsolutePath())) {
            outputInfo = instanceDir.getName();
        }
        CommandUtil.log("Ready to <" + cmdName + "> Qingzhou: " + outputInfo);
        Cmd cmd;
        try {
            cmd = Cmd.valueOf(cmdName);
        } catch (Exception e) {
            CommandUtil.log("Command <" + cmdName + "> not found.");
            return;
        }

        String[] extArgs = Arrays.copyOfRange(args, 2, args.length);
        cmd.exec(instanceDir, new ArrayList<>(Arrays.asList(extArgs)));
        CommandUtil.log("Command <" + cmdName + "> has been executed.");
    }
}
