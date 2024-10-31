package qingzhou.console.controller.rest;

import qingzhou.api.Response;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.*;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.logger.Logger;
import qingzhou.registry.ModelActionInfo;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PageFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) {
        RequestImpl request = context.request;
        ModelActionInfo actionInfo = request.getCachedModelInfo().getModelActionInfo(request.getAction());
        String actionPage = actionInfo.getAppPage();
        if (Utils.isBlank(actionPage)) return true;

        File appPageCacheDir = FileUtil.newFile(SystemController.getModuleContext().getTemp(), DeployerConstants.DOWNLOAD_PAGE_ROOT_DIR, request.getApp());
        File actionPageFile = FileUtil.newFile(appPageCacheDir, actionPage);
        if (actionPageFile.exists()) return true;

        try {
            return requestPage(request.getApp(), appPageCacheDir, actionPage);
        } catch (IOException e) {
            context.resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
    }

    private boolean requestPage(String targetAppName, File appPageCacheDir, String actionPage) throws IOException {
        RequestImpl fileReq = new RequestImpl();
        fileReq.setAppName(DeployerConstants.APP_SYSTEM);
        fileReq.setModelName(DeployerConstants.MODEL_AGENT);
        fileReq.setActionName(DeployerConstants.ACTION_DOWNLOAD_PAGE);
        fileReq.setParameter(DeployerConstants.DOWNLOAD_PAGE_APP, targetAppName);
        String pageRootDirName = getPageRootDirName(actionPage);
        fileReq.setParameter(DeployerConstants.DOWNLOAD_PAGE_DIR, pageRootDirName);

        Map<String, Response> invokeOnInstances = SystemController.getService(ActionInvoker.class)
                .invokeOnInstances(fileReq, SystemController.getAppInstances(targetAppName).get(0));
        Response next = invokeOnInstances.values().iterator().next();
        ResponseImpl res = (ResponseImpl) next;
        if (res.isSuccess() && res.getBodyBytes() != null) {
            File tempFile = FileUtil.newFile(appPageCacheDir, pageRootDirName + ".zip");
            try {
                FileUtil.writeFile(tempFile, res.getBodyBytes(), false);
                FileUtil.unZipToDir(tempFile, appPageCacheDir);
                SystemController.getService(Deployer.class).addAppListener(new AppListener() {
                    @Override
                    public void onInstalled(String appName) {
                    }

                    @Override
                    public void onUninstalled(String appName) {
                        if (appName.equals(targetAppName)) {
                            try {
                                FileUtil.forceDelete(appPageCacheDir);
                            } catch (IOException e) {
                                SystemController.getService(Logger.class).warn(e.getMessage(), e);
                            }
                        }
                    }
                });
            } finally {
                FileUtil.forceDelete(tempFile);
            }
        }

        return true;
    }

    private String getPageRootDirName(String path) {
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
