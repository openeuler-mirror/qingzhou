package qingzhou.console.controller.rest;

import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.ModelActionInfo;

import java.io.File;

public class RedirectFilter implements Filter<RestContext> {
    static {
        I18n.addKeyI18n("redirect_not_exist", new String[]{"重定向 %s 资源不存在: %s", "en:The redirect %s resource does not exist: %s"});
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;
        ModelActionInfo actionInfo = request.getCachedModelInfo().getModelActionInfo(request.getAction());
        String pageForward = actionInfo.getRedirect();
        if (Utils.notBlank(pageForward)) {
            try {
                File appTempDir = FileUtil.newFile(SystemController.getModuleContext().getTemp(), DeployerConstants.APP_WEB_RESOURCES_ROOT_DIR, request.getApp());
                File pageToForward = FileUtil.newFile(appTempDir, pageForward);
                if (!pageToForward.exists()) { // 文件不存在，去远程拉取
                    RequestImpl fileReq = new RequestImpl();
                    fileReq.setAppName(DeployerConstants.APP_SYSTEM);
                    fileReq.setModelName(DeployerConstants.MODEL_AGENT);
                    fileReq.setActionName(DeployerConstants.DOWNLOAD_REDIRECT_ACTION_NAME);
                    String redirect = getFirstLevelDirectory(pageForward);
                    fileReq.setNonModelParameter(DeployerConstants.DOWNLOAD_REDIRECT_DIR_NAME, redirect);
                    fileReq.setNonModelParameter(DeployerConstants.DOWNLOAD_REDIRECT_APP_NAME, request.getApp());
                    ResponseImpl res = (ResponseImpl) SystemController.getService(ActionInvoker.class).invokeOnInstances(fileReq, SystemController.getAppInstances(request.getApp()).get(0)).get(0);
                    if (res.isSuccess() && res.getBodyBytes() != null) {
                        File zipFile = FileUtil.newFile(appTempDir, redirect + ".zip");
                        FileUtil.writeFile(zipFile, res.getBodyBytes(), false);
                        FileUtil.unZipToDir(zipFile, appTempDir);
                        FileUtil.forceDelete(zipFile);
                    }
                    if (!pageToForward.exists()) {
                        throw new RuntimeException("redirect not exist");
                    }
                }
            } catch (Exception e) {
                String i18n = I18n.getKeyI18n("redirect_not_exist", pageForward, e.getMessage());
                request.getResponse().setSuccess(false);
                request.getResponse().setMsg(i18n);
                return false;
            }
        }

        return true;
    }

    private String getFirstLevelDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        String[] parts = path.split("[/\\\\]+");

        for (String part : parts) {
            if (!part.isEmpty()) {
                return part;
            }
        }

        return "";
    }
}
