package qingzhou.app.node;

import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;

import java.io.File;

@Model(name = "nodeagent", icon = "",
        showToMenu = false,
        nameI18n = {"节点代理", "en:Node Agent"},
        infoI18n = {"执行管理节点的指令，如安装和卸载应用等。",
                "en:Execute commands from the management node, such as installing and uninstalling applications."})
public class NodeAgent extends ModelBase {

    @ModelAction(name = "install-app",
            nameI18n = {"部署应用", "en:Install App"},
            infoI18n = {"在该节点上部署应用。", "en:Deploy the application on the node."})
    public void installApp(Request request, Response response) throws Exception {
        File srcFile;
        if (Boolean.parseBoolean(request.getParameter("appFrom"))) {
            srcFile = FileUtil.newFile(request.getParameter("fromUpload"));
        } else {
            srcFile = new File(request.getParameter("filename"));
        }
        if (!srcFile.exists() || !srcFile.isFile()) {
            response.setSuccess(false);
            response.setMsg("File Not Found.");
            return;
        }

        String srcFileName = srcFile.getName();
        String appName;
        File app;
        if (srcFile.isDirectory()) {
            appName = srcFileName;
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.copyFileOrDirectory(srcFile, app);
        } else if (srcFileName.endsWith(".jar")) {
            int index = srcFileName.lastIndexOf(".");
            appName = srcFileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.copyFileOrDirectory(srcFile, FileUtil.newFile(app, "lib", srcFileName));
        } else if (srcFileName.endsWith(".zip")) {
            int index = srcFileName.lastIndexOf(".");
            appName = srcFileName.substring(0, index);
            app = FileUtil.newFile(getAppsDir(), appName);
            FileUtil.unZipToDir(srcFile, app);
        } else {
            throw ExceptionUtil.unexpectedException("unknown app type");
        }

        Main.getFc().getAppManager().installApp(appName, app);
    }

    @ModelAction(name = "uninstall-app",
            nameI18n = {"卸载应用", "en:UnInstall App"},
            infoI18n = {"从该节点上卸载应用。", "en:Uninstall the app from the node."})
    public void unInstallApp(Request request, Response response) throws Exception {
        Main.getFc().getAppManager().unInstallApp(request.getId());
    }

    private File getAppsDir() {
        File apps = FileUtil.newFile(getAppContext().getDomain(), "apps");
        if (!apps.exists()) {
            FileUtil.mkdirs(apps);
        }

        return apps;
    }
}
