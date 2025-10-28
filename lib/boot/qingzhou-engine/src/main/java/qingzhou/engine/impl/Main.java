package qingzhou.engine.impl;

import java.io.File;

import qingzhou.engine.util.pattern.CompositeProcess;

public class Main {
    public static void main(String[] args) throws Throwable {
        String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String flag = "/engine/qingzhou-engine.jar";
        int i = jarPath.indexOf(flag);
        String pre = jarPath.substring(0, i);
        File libDir = new File(pre);

        EngineContext engineContext = new EngineContext(libDir, new File(System.getProperty("qingzhou.instance")));
        engineContext.startArgs = args;
        CompositeProcess sequence = new CompositeProcess(
                new RunningControl(engineContext),
                new ModuleLoading(engineContext)
        );
        try {
            sequence.run();
        } catch (Throwable e) {
            sequence.completed();
            throw e;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(sequence::completed));

        synchronized (Main.class) {
            Main.class.wait();
        }
    }
}
