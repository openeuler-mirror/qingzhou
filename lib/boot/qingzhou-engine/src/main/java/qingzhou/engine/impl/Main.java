package qingzhou.engine.impl;

import qingzhou.engine.util.pattern.CompositeProcess;

public class Main {
    public static void main(String[] args) throws Throwable {
        EngineContext engineContext = new EngineContext();
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
