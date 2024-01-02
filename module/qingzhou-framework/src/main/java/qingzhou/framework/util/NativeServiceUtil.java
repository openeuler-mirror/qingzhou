package qingzhou.framework.util;

import java.io.File;

public class NativeServiceUtil {
    public static String installQZAsService(File home, String domain) throws Exception {
        try {
            //added by chenglin for #ITAIT-4155,在启动脚本里设置jdk环境变量。
            // service运行指定服务时,把大部分环境变量去掉了,所以必须手动设置
            File envFile = FileUtil.newFile(home, "bin", "JAVA_HOME.txt");
            String javaHome = JDKUtil.getJavaHome();
            FileUtil.writeFile(envFile, javaHome);

            File startFilePath = FileUtil.newFile(home, "bin", "startd.sh");
            return installAsService(startFilePath.getCanonicalPath(), domain, javaHome);
        } catch (Throwable t) {
            throw new Exception("Failed to set auto-start server: " + domain, t);
        }
    }

    public static void unInstallService(String serviceName) throws Exception {
        //path为linux自启动服务固定路径,主动拼接service文件名。add for ITAIT-4155
        File serviceFile = serviceFile(serviceName);
        if (serviceFile == null) {
            System.out.println("Setting the automatic boot service on this platform is not supported.");
            return;
        }

        if (OSUtil.IS_WINDOWS || !serviceFile.exists() || !serviceFile.isFile()) {
            return;
        }

        String temp = "systemctl disable " + serviceFile.getName();
        NativeCommandUtil.runNativeCommand(temp, null, null);
        FileUtil.forceDeleteQuietly(serviceFile);

        System.out.println("The service has been uninstalled from: " + serviceFile.getAbsolutePath());
    }

    /**
     * Linux下开机自启动某个服务, startCmdPath服务启动脚本命令路径,serviceName自启动配置service文件名称,eg:xxx.service
     * 返回 null 表示安装成功，否则返回错误消息。
     **/
    public static String installAsService(String startCmdPath, String serviceName, String javaHome) throws Exception {
        if (OSUtil.IS_WINDOWS) {
            return "Failed to install server as a system self-starting service. Only Linux platform is supported.";
        }
        String s = System.lineSeparator();
        String context = "[Unit]" + s + "Description=QingZhou" + s + "After=network.target" + s +

                "[Service]" + s + "Type=forking" + s + (StringUtil.notBlank(javaHome) ? ("Environment=\"JAVA_HOME=" + javaHome + "\"" + s) : "") + "ExecStart=/bin/bash " + startCmdPath + s + //tw8启动服务文件必须加/bin/bash add for ITAIT-4155
                "PrivateTmp=false" + s + "TimeoutSec=0" + s +

                "[Install]" + s + "WantedBy=multi-user.target" + s;

        //path为linux自启动服务固定路径,主动拼接service文件名。add for ITAIT-4155
        File serviceFile = serviceFile(serviceName);
        if (serviceFile == null) {
            System.out.println("Setting the automatic boot service on this platform is not supported.");
            return null;
        }
        FileUtil.writeFile(serviceFile, context);
        String temp = "systemctl enable " + serviceFile.getName();
        NativeCommandUtil.runNativeCommand(temp, null, null);

        System.out.println("The service has been installed to: " + serviceFile.getAbsolutePath());

        return null;
    }


    public static File serviceFile(String domain) {
        String serviceName = domain.endsWith(".service") ? domain : domain + ".service";
        if (new File("/etc/systemd/system").exists()) {
            return FileUtil.newFile("/etc/systemd/system/", serviceName);

        } else if (new File("/usr/lib/systemd/system").exists()) {
            return FileUtil.newFile("/usr/lib/systemd/system/", serviceName);
        }
        return null;
    }

    private NativeServiceUtil() {
    }
}
