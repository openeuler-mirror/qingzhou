package qingzhou.app.nginx;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Menu;
import qingzhou.api.QingzhouApp;

@App(code = "nginx", icon = "Nginx",
        name = {"Nginx应用", "en:Nginx Application"},
        info = {"用于演示轻舟的功能。", "en:Used to demo the ability of Qingzhou."})
@Menu(name = {"基础功能", "en:Basic"}, code = "basic", icon = "cog", order = 1)
public class AppNginx implements QingzhouApp {

    @Override
    public void start(AppContext appContext) {
        System.out.println("\n========================================== nginx ====================================================");
        try {
            appContext.getTemp().mkdirs();
            appContext.getProperties().forEach((k, v) -> System.out.println("nginx 应用配置 " + k + "=" + v));

            // 将 nginx.conf 文件输出到临时目录用作模拟真实的 nginx.conf
            new File(appContext.getTemp(), "backups").mkdirs();
            Path nginxConf = new File(appContext.getTemp(), "nginx.conf").toPath();
            nginxConf.toFile().createNewFile();
            URL nginxConfUrl = getClass().getClassLoader().getResource("nginx.conf");

            try (InputStream in = nginxConfUrl.openStream()) {
                Files.copy(in, nginxConf, StandardCopyOption.REPLACE_EXISTING);
            }

            AppConfig.getConfig().setProperty(AppConfig.NGINX_CONF_PATH_KEY, nginxConf.toString());
            AppConfig.getConfig().setProperty(AppConfig.NGINX_CONF_BACKUPS_KEY, new File(appContext.getTemp(), "backups").getAbsolutePath());

            System.out.println("nginx 应用版本号：" + appContext.getVersion());
            System.out.println("nginx 应用根路径：" + appContext.getBase());
            System.out.println("nginx 应用临时目录：" + appContext.getTemp().getAbsolutePath());
        } catch (IOException | UnsupportedOperationException e) {
            System.err.println("nginx 应用启动异常: " + e.getMessage());
        }
        System.out.println("========================================== nginx ====================================================\n");
    }

    @Override
    public void stop() {
        System.out.println("nginx 应用停止");
    }
}
