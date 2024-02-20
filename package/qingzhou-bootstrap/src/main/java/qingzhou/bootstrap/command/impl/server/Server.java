package qingzhou.bootstrap.command.impl.server;

import qingzhou.bootstrap.Utils;
import qingzhou.bootstrap.command.CommandLineProcessor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class Server extends CommandLineProcessor {
    private static File domainDir;

    public Server() {
        // 实现 CommandLineProcessor 接口，须有无参的公开构造方法
    }

    static void log(String msg) {
        System.out.println(logPrefix() + msg);
    }

    private static String logPrefix() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss >>> ");
        return dateFormat.format(new Date());
    }

    static File getDomainDir() {
        return domainDir;
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
            log("command line argument pattern: <cmd> [domain] [...]");
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
            log("Domain does not exist: " + base.getName());
            return;
        }
        System.setProperty("qingzhou.domain", qzDomain);

        domainDir = new File(qzDomain);

        String cmdName = args[0];
        String outputDomain = qzDomain;
        if (qzDomain.startsWith(Utils.getHome().getCanonicalPath())) {
            outputDomain = new File(qzDomain).getName();
        }
        log("Ready to <" + cmdName + "> Qingzhou: " + outputDomain);
        Cmd cmd;
        try {
            cmd = Cmd.valueOf(cmdName);
        } catch (Exception e) {
            log("Command <" + cmdName + "> not found.");
            return;
        }

        String[] extArgs = Arrays.copyOfRange(args, 2, args.length);
        cmd.exec(domainDir, new ArrayList<>(Arrays.asList(extArgs)));
        log("Command <" + cmdName + "> has been executed.");
    }
}
