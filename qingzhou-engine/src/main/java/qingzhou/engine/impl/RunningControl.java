package qingzhou.engine.impl;

import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;

import java.io.File;
import java.io.IOException;

class RunningControl implements Process {
    private final EngineContext engineContext;
    private File running;

    RunningControl(EngineContext engineContext) {
        this.engineContext = engineContext;
    }

    @Override
    public void exec() throws Exception {
        // 实例不可重复启动，因为端口和 temp 文件都会冲突
        running = Utils.newFile(engineContext.getInstanceDir(), "temp", "running");
        if (running.exists() && checkService()) {
            throw new IllegalStateException("QingZhou is already starting");
        }

        Utils.mkdirs(running.getParentFile());
        if (!running.exists() && !running.createNewFile()) {
            throw new IllegalStateException("failed to create new file: " + running);
        }

        // 正常启动之前先清理上次启动的缓存文件
        Utils.forceDelete(engineContext.getTemp());
    }

    private boolean checkService() {
        return false;// todo: 需可靠的校验方式，通信校验？
    }

    @Override
    public void undo() {
        try {
            Utils.forceDelete(running);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println(Utils.stackTraceToString(e.getStackTrace()));
        }
    }
}
