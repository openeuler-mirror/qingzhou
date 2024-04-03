package qingzhou.command.server;

import qingzhou.command.Utils;
import qingzhou.command.CommandLineProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class Server extends CommandLineProcessor {
    public Server() {
        // 实现 CommandLineProcessor 接口，须有无参的公开构造方法
    }

    @Override
    public String name() {
        return "server";
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
            Utils.log("command line argument pattern: <cmd> [domain] [...]");
            return;
        }

        if (args.length == 1) {
            args = new String[]{args[0], "domain1"};
        }

        String qzDomain = args[1];
        File base = new File(qzDomain);
        if (base.isAbsolute()) {
            qzDomain = base.getCanonicalPath();
        } else {
            qzDomain = Utils.getDomain(qzDomain).getCanonicalPath();
        }

        if (!new File(qzDomain).isDirectory()) {
            Utils.log("Domain does not exist: " + base.getName());
            return;
        }
        System.setProperty("qingzhou.domain", qzDomain);

        String cmdName = args[0];
        String outputDomain = qzDomain;
        if (qzDomain.startsWith(Utils.getHome().getCanonicalPath())) {
            outputDomain = new File(qzDomain).getName();
        }
        Utils.log("Ready to <" + cmdName + "> Qingzhou: " + outputDomain);
        Cmd cmd;
        try {
            cmd = Cmd.valueOf(cmdName);
        } catch (Exception e) {
            Utils.log("Command <" + cmdName + "> not found.");
            return;
        }

        String[] extArgs = Arrays.copyOfRange(args, 2, args.length);
        cmd.exec(new File(qzDomain), new ArrayList<>(Arrays.asList(extArgs)));
        Utils.log("Command <" + cmdName + "> has been executed.");
    }
}
