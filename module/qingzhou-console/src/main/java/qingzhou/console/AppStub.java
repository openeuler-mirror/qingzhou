package qingzhou.console;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.util.ExceptionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static qingzhou.console.impl.ConsoleWarHelper.getAppManager;

public class AppStub {
    public static final Map<String, ConsoleContext> appStubMap = new ConcurrentHashMap<>();

    public static ConsoleContext getConsoleContext(String appName) {
        return appStubMap.computeIfAbsent(appName, s -> {
            List<String> nodes = getAppNodes(s);
            if (nodes.contains(FrameworkContext.LOCAL_NODE_NAME)) {
                return getAppManager().getAppInfo(s).getAppContext().getConsoleContext();
            } else {
                for (String node : nodes) {
                    try {
                        // todo : 尝试发送远程获取 ConsoleContext 对象，获取到一个即结束！
                        return null;
                    } catch (Exception ignored) {
                    }
                }
                throw ExceptionUtil.unexpectedException("App [ " + s + " ] not found.");
            }
        });
    }

    public static List<String> getAppNodes(String appName) {
        List<String> nodes = new ArrayList<>();
        if (FrameworkContext.MASTER_APP_NAME.equals(appName)) {
            nodes.add(FrameworkContext.LOCAL_NODE_NAME);
        } else {
            Map<String, String> app;
            try {
                app = getAppManager().getAppInfo(FrameworkContext.MASTER_APP_NAME)
                        .getAppContext().getDataStore()
                        .getDataById(ConsoleConstants.MODEL_NAME_app, appName);
            } catch (Exception e) {
                throw ExceptionUtil.unexpectedException(e);
            }
            if (app == null || app.isEmpty()) {
                throw ExceptionUtil.unexpectedException("App [ " + appName + " ] not found.");
            }
            String[] appNodes = app.get("nodes").split(",");
            nodes.addAll(Arrays.asList(appNodes));
        }
        return nodes;
    }

    private AppStub() {
    }
}
