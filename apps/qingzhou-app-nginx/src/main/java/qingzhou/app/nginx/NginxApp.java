package qingzhou.app.nginx;

import java.io.File;

import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.api.annotations.App;
import qingzhou.api.annotations.I18n;
import qingzhou.logger.Logger;

@I18n(name = {"基本信息", "en:Basic"}, code = "Basic")
@I18n(name = {"配置信息", "en:Configuration"}, code = "Config")
@App(code = "nginx", icon = "/icons/nginx.svg", roles = "admin",
        name = {"Nginx管理", "en:Nginx Manager"},
        info = {"本机 nginx 日常运维：状态查看、启停重载、配置校验与编辑、日志查看。",
                "en:Manage local nginx: status, start/stop/reload, config test/edit and logs."})
public class NginxApp implements QingzhouApp {

    @Override
    public void start(AppContext appContext) {
        Logger logger = appContext.getService(Logger.class);
        try {
            NginxFiles.recoverPendingBackups(new File(appContext.getTempDir(), "backups").toPath());
        } catch (RuntimeException e) {
            if (logger != null) logger.error("nginx 配置恢复失败", e);
        }

        try {
            NginxInstall install = NginxInstall.resolve(appContext);
            if (logger != null) {
                logger.info("nginx 管理应用启动，安装目录: " + NginxInstall.absolute(install.prefix));
            }
        } catch (NginxInstall.NotFoundException e) {
            // 未找到 nginx 不阻止应用加载：概览页需要展示配置提示，补齐路径后无需重启轻舟
            if (logger != null) logger.warn("nginx 管理应用未定位到 nginx: " + e.getMessage());
        }
    }
}
