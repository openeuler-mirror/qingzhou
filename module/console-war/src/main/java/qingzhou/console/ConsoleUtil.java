package qingzhou.console;

import qingzhou.framework.api.DownloadModel;

public class ConsoleUtil {// todo 临时工具类，后续考虑移除
    private static Boolean disableDownload;

    private ConsoleUtil() {
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
}
