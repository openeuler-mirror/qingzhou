package qingzhou.engine.impl;

import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RunningControl implements Process {
    private final EngineContext engineContext;
    private File running;

    RunningControl(EngineContext engineContext) {
        this.engineContext = engineContext;
    }

    @Override
    public void exec() throws Exception {
        // 实例不可重复启动，因为端口和 temp 文件都会冲突
        running = FileUtil.newFile(engineContext.getInstanceDir(), "temp", "running");
        if (running.exists() && checkService()) {
            throw new IllegalStateException("Qingzhou is already starting");
        }

        FileUtil.mkdirs(running.getParentFile());
        if (!running.exists() && !running.createNewFile()) {
            throw new IllegalStateException("failed to create new file: " + running);
        }

        // 正常启动之前先清理上次启动的缓存文件
        FileUtil.forceDelete(engineContext.getTemp());
    }

    private boolean checkService() {
        String jsonConfig;
        try {
            jsonConfig = FileUtil.fileToString(FileUtil.newFile(engineContext.getInstanceDir(), "conf", "qingzhou.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String[] checkPorts = new String[]{"port", "agentPort"};
        return Arrays.stream(checkPorts).anyMatch(checkPort -> checkPort(checkPort, jsonConfig));
    }

    private boolean checkPort(String portPattern, String content) {
        String portRegex = "\"" + portPattern + "\":\\s*\"(\\d+)\"";
        Matcher matcher = Pattern.compile(portRegex).matcher(content);
        while (matcher.find()) {
            String port = matcher.group(1);
            try (Socket ignored1 = new Socket("localhost", Integer.parseInt(port))) {
                System.out.println("Qingzhou may have started, because the designated port:" + port + " is in use!");
                return true;
            } catch (Throwable ignored2) {
            }
        }
        return false;
    }

    @Override
    public void undo() {
        try {
            FileUtil.forceDelete(running);
        } catch (IOException e) {
            System.err.println(Utils.exceptionToString(e));
        }
    }
}
