package qingzhou.engine.impl;

import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
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
        String regex = "\"port\":\\s*\"(\\d+)\"";
        String agentPortRegex = "\"agentPort\":\\s*\"(\\d+)\"";
        Pattern pattern = Pattern.compile(regex);
        Pattern patternAgent = Pattern.compile(agentPortRegex);
        Matcher matcher = pattern.matcher(jsonConfig);
        Matcher matcherAgent = patternAgent.matcher(jsonConfig);

        while (matcher.find()) {
            String port = matcher.group(1); // 获取 port 的值
            if(checkPort(port)){
                return true;
            }
        }
        while (matcherAgent.find()) {
            String port = matcherAgent.group(1);
            if(checkPort(port)){
                return true;
            }
        }
        return false;
    }

    private boolean checkPort(String port) {
        try {

            try (Socket socket = new Socket("localhost", Integer.parseInt(port))) {
                return true;
                //System.out.println("Port " + finalPort + " is open");
            } catch (IOException e) {

                return false;
            }
            // 端口关闭或无法连接
        } catch (Exception e) {
            throw new IllegalStateException("failed to check port!");
        }

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
