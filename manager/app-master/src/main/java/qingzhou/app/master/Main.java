package qingzhou.app.master;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Editable;
import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.framework.app.AppInfo;
import qingzhou.framework.app.QingzhouSystemApp;

import java.util.HashMap;
import java.util.Map;

public class Main extends QingzhouSystemApp {
    private static FrameworkContext FC;

    @Override
    public void start(AppContext appContext) {
        FC = this.frameworkContext;

        appContext.addI18n("validator.master.system", new String[]{"为保障系统安全可用，请勿修改此配置", "en:To ensure the security and availability of the system, do not modify this configuration"});
        appContext.addI18n("client.trusted.not", new String[]{"该操作仅限于在服务器本机或受信任的IP上执行，受信任IP的设置方式请参考产品手册", "en:This operation can only be performed on the local server or on a trusted IP. Please refer to the product manual for the setting method of the trusted IP"});

        appContext.addMenu("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        appContext.addMenu("System", new String[]{"系统管理", "en:System"}, "cog", 2);
        appContext.addMenu("Guide", new String[]{"用户指引", "en:Guide"}, "hand-up", 3);

        appContext.setDefaultDataStore(new ConsoleDataStore());

        appContext.addActionFilter(new LocalNodeProtection());// 禁止修改本地节点
    }

    public static <T> T getService(Class<T> type) {
        return FC.getService(type);
    }

    public static FrameworkContext getFramework() {
        return FC;
    }

    public static Map<String, String> prepareParameters(Request request, AppContext appContext) {
        Map<String, String> properties = new HashMap<>();
        String[] fieldNames = appContext.getAppMetadata().getModelManager().getFieldNames(request.getModelName());
        for (String fieldName : fieldNames) {
            String value = request.getParameter(fieldName);
            if (value != null) {
                properties.put(fieldName, value);
            }
        }
        return properties;
    }

    private static class LocalNodeProtection implements ActionFilter {

        @Override
        public String doFilter(Request request, Response response, AppContext appContext) {
            if (AppInfo.SYS_MODEL_NODE.equals(request.getModelName()) && AppInfo.SYS_NODE_LOCAL.equals(request.getId())) {
                if (Editable.ACTION_NAME_UPDATE.equals(request.getActionName())
                        || Deletable.ACTION_NAME_DELETE.equals(request.getActionName())) {
                    return appContext.getAppMetadata().getI18n(request.getI18nLang(), "validator.master.system");
                }
            }

            return null;
        }
    }
}
