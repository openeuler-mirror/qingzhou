package qingzhou.app.master;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Editable;
import qingzhou.deployer.QingzhouSystemApp;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@qingzhou.api.App
public class MasterApp extends QingzhouSystemApp {
    private static MasterApp masterApp;

    @Override
    public void start(AppContext appContext) {
        masterApp = this;

        appContext.addI18n("validator.master.system", new String[]{"为保障系统安全可用，请勿修改此配置", "en:To ensure the security and availability of the system, do not modify this configuration"});
        appContext.addI18n("client.trusted.not", new String[]{"该操作仅限于在服务器本机或受信任的IP上执行，受信任IP的设置方式请参考产品手册", "en:This operation can only be performed on the local server or on a trusted IP. Please refer to the product manual for the setting method of the trusted IP"});

        appContext.addMenu("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        appContext.addMenu("System", new String[]{"系统管理", "en:System"}, "cog", 2);

        appContext.addActionFilter(new LocalNodeProtection(appContext));// 禁止修改本地节点
    }

    public static String getInstanceId() {
        return masterApp.moduleContext.getInstanceDir().getName();
    }

    public static <T> T getService(Class<T> type) {
        List<Field> collect = Arrays.stream(QingzhouSystemApp.class.getDeclaredFields()).filter(field -> field.getType() == type).collect(Collectors.toList());
        try {
            return (T) collect.get(0).get(masterApp);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static class LocalNodeProtection implements ActionFilter {
        private final AppContext appContext;

        private LocalNodeProtection(AppContext appContext) {
            this.appContext = appContext;
        }

        @Override
        public String doFilter(Request request, Response response) {
            if ("instance".equals(request.getModel()) && "local".equals(request.getId())) {
                if (Editable.ACTION_NAME_UPDATE.equals(request.getAction())
                        || Deletable.ACTION_NAME_DELETE.equals(request.getAction())) {
                    return appContext.getI18n(request.getLang(), "validator.master.system");
                }
            }

            return null;
        }
    }
}
