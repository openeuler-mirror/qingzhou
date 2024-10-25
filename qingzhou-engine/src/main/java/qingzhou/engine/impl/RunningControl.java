package qingzhou.engine.impl;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;

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
        if (checkService()) {
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
        return checkPort()&&checkTmp();
    }

    private boolean checkPort() {
        try {
            String jsonConfig=FileUtil.fileToString(FileUtil.newFile(engineContext.getInstanceDir(),"conf","qingzhou.json"));
            String regex = "\"port\":\\s*\"(\\d+)\"";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(jsonConfig);
            String port="9000";

            if (matcher.find()) {
                //String enabled = matcher.group(1); // 获取 enabled 的值
                port = matcher.group(1); // 获取 port 的值
            } else {
                System.out.println("未找到相关配置");
            }
                URL url = new URL("http://localhost:"+port+"/console");

                // 打开连接
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 设置请求方法为GET
                connection.setRequestMethod("GET");

                // 设置连接超时时间和读取超时时间（单位：毫秒）
                connection.setConnectTimeout(1000); // 5秒
                connection.setReadTimeout(1000); // 5秒

            connection.disconnect();
            int responseCode = connection.getResponseCode();

                // 如果响应码是200（HTTP_OK），则表示连接成功
                if (responseCode == 200) {
                    return true;
                } else {
                    return false;
                }
            }
        catch (ConnectException e){
            return false;
        }
        catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }
    private boolean checkTmp(){
        //检查当前是否存在临时文件
        File temp=FileUtil.newFile(engineContext.getTemp());
        //System.out.println(temp.getAbsolutePath());
        //System.out.println(temp.exists());
        //System.out.println(temp);
        return temp.exists();}




    @Override
    public void undo() {
        try {
            FileUtil.forceDelete(running);
        } catch (IOException e) {
            System.err.println(Utils.exceptionToString(e));
        }
    }
}
