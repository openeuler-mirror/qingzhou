package qingzhou.console;

import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.page.PageBackendService;
import qingzhou.framework.api.DownloadModel;
import qingzhou.framework.api.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ConsoleUtil {// todo 临时工具类，后续考虑移除
    public static String ACTION_NAME_SERVER = "server";
    public static String ACTION_NAME_TARGET = "target";
    public static String ACTION_NAME_validate = "validate";
    private static Boolean disableDownload;

    private ConsoleUtil() {
    }

    public static String buildRequestUrl(HttpServletRequest servletRequest, HttpServletResponse response, Request request, String viewName, String actionName) {
        String url = servletRequest.getContextPath() + RESTController.REST_PREFIX + "/" + viewName + "/" + request.getAppName() + "/" + request.getModelName() + "/" + actionName;
        return response.encodeURL(url);
    }

    public static boolean isDisableDownload() {
        if (disableDownload == null) {
            disableDownload = ServerXml.get().isDisableDownload();
        }

        return disableDownload;
    }

    public static boolean isDisable(String action) {
        if (DownloadModel.ACTION_NAME_DOWNLOADLIST.equals(action)) {
            return isDisableDownload();
        } else {
            return false;
        }
    }

    public static String encodeRedirectURL(HttpServletRequest request, HttpServletResponse response, String url) {
        return response.encodeURL(PageBackendService.encodeTarget(request, url));
    }

    public static String retrieveServletPathAndPathInfo(HttpServletRequest request) {
        return request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
    }

}
