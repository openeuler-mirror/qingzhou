package qingzhou.engine.impl;

import java.io.File;
import java.io.IOException;
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
        return checkConnect()||checkTmp();
    }

    private boolean checkConnect() {
        try {
            File cfg = FileUtil.newFile(engineContext.getInstanceDir(), "conf", "qingzhou.json");
            String jsonConfig = FileUtil.fileToString(cfg);
            // 改进后的正则表达式，匹配整个 console 配置段落
            String regex = "\"module\":\\s*\\{\\s*\"console\":\\s*\\{\\s*\"enabled\":\\s*\"(.*?)\",\\s*\"port\":\\s*\"(\\d+)\"";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(jsonConfig);

            if (matcher.find()) {
                //String enabled = matcher.group(1); // 获取 enabled 的值
                String port = matcher.group(2); // 获取 port 的值
                //System.out.println("Enabled: " + enabled);
                System.out.println("current port ==" + port);
                String url = "http://localhost:9000/console";
                boolean isConnected = checkConnection(url);
                //System.out.println("连接成功: " + isConnected);
                return isConnected;
            } else {
                System.out.println("未找到相关配置");
                return false;
            }
        }
        catch (IOException e) {
            return false;
        }
        //return false;
    }
    private boolean checkTmp(){
        //检查当前是否存在临时文件
        File temp=FileUtil.newFile(engineContext.getTemp());
        //System.out.println(temp.getAbsolutePath());
        //System.out.println(temp.exists());
        //System.out.println(temp);
        return temp.exists();}
    private static boolean checkConnection(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000); // 设置连接超时时间
            connection.setReadTimeout(2000);    // 设置读取超时时间

            int responseCode = connection.getResponseCode();
            return (responseCode == HttpURLConnection.HTTP_OK); // 判断是否为200 OK
        } catch (IOException e) {
            //e.printStackTrace();
            return false; // 连接失败
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
