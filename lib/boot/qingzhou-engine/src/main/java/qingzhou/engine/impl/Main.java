package qingzhou.engine.impl;

import qingzhou.engine.util.pattern.ProcessSequence;

public class Main {
    public static void main(String[] args) throws Exception {
        EngineContext engineContext = new EngineContext();
        ProcessSequence sequence = new ProcessSequence(
                new RunningControl(engineContext),
                new ModuleLoading(engineContext)
        );
        try {
            sequence.exec();
        } catch (Exception e) {
            sequence.undo();
            throw e;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(sequence::undo));
        synchronized (Main.class) {
            Main.class.wait();
        }
    }
}
