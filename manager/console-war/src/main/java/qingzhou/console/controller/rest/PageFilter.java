package qingzhou.console.controller.rest;

import qingzhou.console.controller.SystemController;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.ModelActionInfo;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PageFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) {
        RequestImpl request = context.request;
        ModelActionInfo actionInfo = request.getCachedModelInfo().getModelActionInfo(request.getAction());
        String actionPage = actionInfo.getPage();
        if (Utils.isBlank(actionPage)) return true;

        File appPageCacheDir = FileUtil.newFile(SystemController.getModuleContext().getTemp(), DeployerConstants.DOWNLOAD_PAGE_ROOT_DIR, request.getApp());
        File actionPageFile = FileUtil.newFile(appPageCacheDir, actionPage);
        if (actionPageFile.exists()) return true;

        RequestImpl fileReq = new RequestImpl();
        fileReq.setAppName(DeployerConstants.APP_SYSTEM);
        fileReq.setModelName(DeployerConstants.MODEL_AGENT);
        fileReq.setActionName(DeployerConstants.ACTION_DOWNLOAD_PAGE);
        fileReq.setNonModelParameter(DeployerConstants.DOWNLOAD_PAGE_APP, request.getApp());
        String pageRootDirName = getPageRootDirName(actionPage);
        fileReq.setNonModelParameter(DeployerConstants.DOWNLOAD_PAGE_DIR, pageRootDirName);

        try {
            ResponseImpl res = (ResponseImpl) SystemController.getService(ActionInvoker.class)
                    .invokeOnInstances(fileReq, SystemController.getAppInstances(request.getApp()).get(0)).get(0);
            if (res.isSuccess() && res.getBodyBytes() != null) {
                File tempFile = FileUtil.newFile(appPageCacheDir, pageRootDirName + ".zip");
                try {
                    FileUtil.writeFile(tempFile, res.getBodyBytes(), false);
                    FileUtil.unZipToDir(tempFile, appPageCacheDir);
                } finally {
                    FileUtil.forceDelete(tempFile);
                }
            }
            if (!actionPageFile.exists()) {
                throw new FileNotFoundException(actionPageFile.getPath());
            }
        } catch (IOException e) {
            context.resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
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
