package qingzhou.app.nginx;

import java.io.File;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Menu;
import qingzhou.api.QingzhouApp;
import qingzhou.logger.Logger;

@App(code = "nginx", icon = "Nginx",
        name = {"Nginx应用", "en:Nginx Application"},
        info = {"用于演示轻舟的功能。", "en:Used to demo the ability of Qingzhou."})
@Menu(name = {"基础功能", "en:Basic"}, code = "basic", icon = "cog", order = 1)
public class AppNginx implements QingzhouApp {
    private Logger logger;

    @Override
    public void start(AppContext appContext) {
        logger = appContext.getService(Logger.class);

        try {
            /* 配置 instances/default/conf/qingzhou.properties
            # nginx 安装根目录
            app~qingzhou-nginx-app.nginx.path=/usr/loca/nginx
            # nginx.conf 路径（注意：程序需要有读写 nginx.conf 文件的权限）
            #app~qingzhou-nginx-app.nginx.path.conf=/usr/local/nginx/conf/nginx.conf
            # nginx.conf 备份路径（注意：程序需要有读写该目录的权限）
            #app~qingzhou-nginx-app.nginx.path.backups=/usr/local/nginx/conf/backups
            # nginx 监控路径
            app~qingzhou-nginx-app.nginx_status_url=http://localhost:9000/nginx_status
            */
            AppConfig.getConfig().putAll(appContext.getProperties());
            if (!"".equals(AppConfig.getNginxPath()) && new File(AppConfig.getNginxPath()).exists()) {
                appContext.getTemp().mkdirs();
                // 创建 nginx.conf 文件备份目录到临时目录用作模拟真实的 nginx.conf
                File nginxConfBackups = new File(appContext.getBase(), "nginx-backups");
                nginxConfBackups.mkdirs();

                File nginxPath = new File(AppConfig.getNginxPath());
                AppConfig.getConfig().setProperty(AppConfig.NGINX_CONF_PATH_KEY, new File(nginxPath, "conf/nginx.conf").getAbsolutePath());
                AppConfig.getConfig().setProperty(AppConfig.NGINX_CONF_BACKUPS_KEY, nginxConfBackups.getAbsolutePath());
                logger.info("检测到本地已安装 nginx 应用：" + nginxPath.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("nginx 应用启动异常: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        logger.info("nginx 应用停止");
    }
}
