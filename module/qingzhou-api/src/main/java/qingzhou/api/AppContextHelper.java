package qingzhou.api;

public class AppContextHelper { // todo 最后发布需要去掉这个工具类
    private static AppContext appContext;

    public static AppContext getAppContext() {
        return appContext;
    }

    public static synchronized void setAppContext(AppContext appContext) {
        AppContextHelper.appContext = appContext;
    }

    private AppContextHelper() {
    }
}
