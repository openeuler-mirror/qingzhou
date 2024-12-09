package qingzhou.console.view;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.Response;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.core.AppPageData;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.AppListener;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.engine.util.FileUtil;
import qingzhou.logger.Logger;

class AppPageUtil {
    static void doPage(String actionPage, RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        HttpServletRequest servletRequest = restContext.req;
        HttpServletResponse servletResponse = restContext.resp;

        File appPageCacheDir = FileUtil.newFile(SystemController.getModuleContext().getTemp(), AppPageData.DOWNLOAD_PAGE_ROOT_DIR, request.getApp());
        File actionPageFile = FileUtil.newFile(appPageCacheDir, actionPage);
        if (!actionPageFile.exists()) {
            requestPage(request, appPageCacheDir, actionPage);
        }

        try {
            String page = "/" + request.getApp() + (actionPage.startsWith("/") ? actionPage : "/" + actionPage);
            servletRequest.getRequestDispatcher(page).forward(servletRequest, servletResponse);
        } catch (Exception e) {
            servletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw e;
        }
    }

    private static void requestPage(RequestImpl request, File appPageCacheDir, String actionPage) throws IOException {
        RequestImpl fileReq = new RequestImpl(request);
        String targetAppName = request.getApp();
        fileReq.setAppName(DeployerConstants.APP_SYSTEM);
        fileReq.setModelName(DeployerConstants.MODEL_AGENT);
        fileReq.setActionName(AppPageData.ACTION_DOWNLOAD_PAGE);
        fileReq.getParameters().put(AppPageData.DOWNLOAD_PAGE_APP, targetAppName);
        String pageRootDirName = getPageRootDirName(actionPage);
        fileReq.getParameters().put(AppPageData.DOWNLOAD_PAGE_DIR, pageRootDirName);

        Map<String, Response> invokeOnInstances = SystemController.getService(ActionInvoker.class)
                .invokeOnInstances(fileReq, SystemController.getAppInstances(targetAppName).get(0));
        Response next = invokeOnInstances.values().iterator().next();
        ResponseImpl res = (ResponseImpl) next;
        if (res.isSuccess() && res.getInternalData() != null) {
            File tempFile = FileUtil.newFile(appPageCacheDir, pageRootDirName + ".zip");
            try {
                FileUtil.writeFile(tempFile, (byte[]) res.getInternalData(), false);
                FileUtil.unZipToDir(tempFile, appPageCacheDir);
                deleteOnUninstall(targetAppName, appPageCacheDir);
            } finally {
                FileUtil.forceDelete(tempFile);
            }
        }
    }

    private static void deleteOnUninstall(String app, File appPageCacheDir) {
        Deployer deployer = SystemController.getService(Deployer.class);
        deployer.addAppListener(new AppListener() {
            @Override
            public void onUninstalled(String appName) {
                if (appName.equals(app)) {
                    deployer.removeAppListener(this);
                    try {
                        FileUtil.forceDelete(appPageCacheDir);
                    } catch (IOException e) {
                        SystemController.getService(Logger.class).warn(e.getMessage(), e);
                    }
                }
            }
        });
    }

    private static String getPageRootDirName(String path) {
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
