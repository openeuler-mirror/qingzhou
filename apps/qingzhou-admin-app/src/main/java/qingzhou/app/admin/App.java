package qingzhou.app.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.*;
import qingzhou.api.type.List;
import qingzhou.api.type.Show;
import qingzhou.dto.meta.AppMeta;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Registry;

@Model(code = "app", order = 1,
        name = {"应用", "en:App"},
        info = {"管理所有应用", "en:Manage all applications"},
        menu = "service")
public class App extends ModelBase implements List, Show {
    private Registry registry;

    @ModelField(id = true,
            name = {"ID", "en:ID"},
            info = {"应用唯一标识", "en:Application unique identifier"},
            list = true)
    public String id;

    @ModelField(
            name = {"名称", "en:Name"},
            info = {"应用名称", "en:Application name"},
            list = true,
            search = true,
            add = false,
            update = false)
    public String name;

    @ModelField(
            name = {"代码", "en:Code"},
            info = {"应用代码", "en:Application code"},
            list = true,
            search = true,
            add = false,
            update = false)
    public String code;

    @ModelField(
            name = {"实例", "en:Instance"},
            info = {"所属实例", "en:Instance ID"},
            list = true,
            add = false,
            update = false)
    public String instance;

    @ModelField(
            name = {"图标", "en:Icon"},
            info = {"应用图标", "en:Application icon"},
            add = false,
            update = false)
    public String icon;

    @ModelField(
            name = {"描述", "en:Description"},
            info = {"应用描述", "en:Application description"},
            add = false,
            update = false)
    public String info;

    @ModelField(
            name = {"主机", "en:Host"},
            info = {"实例主机地址", "en:Instance host address"},
            list = true,
            add = false,
            update = false)
    public String host;

    @Override
    public java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> allApps = getAllApps(query);

        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, allApps.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> app = allApps.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                data[j] = app.getOrDefault(listFields[j], "");
            }
            result.add(data);
        }

        return result;
    }

    @Override
    public int totalSize(Map<String, String> query) {
        return getAllApps(query).size();
    }

    @Override
    public boolean contains(String id) {
        return getAppById(id) != null;
    }

    @Override
    public Map<String, String> show(Request request) {
        return getAppById(request.getId());
    }

    private Registry getRegistry() {
        if (registry == null) {
            synchronized (this) {
                if (registry == null) {
                    registry = getAppContext().getService(Registry.class);
                    if (registry == null) {
                        throw new IllegalStateException("Registry service is not available");
                    }
                }
            }
        }
        return registry;
    }

    private java.util.List<Map<String, String>> getAllApps(Map<String, String> query) {
        java.util.List<Map<String, String>> apps = new ArrayList<>();

        for (String localApp : getRegistry().getAllLocalApps()) {
            Map<String, String> appData = getAppData(Constants.LOCAL_INSTANCE_ID, localApp);
            if (matchesQuery(appData, query)) {
                apps.add(appData);
            }
        }

        for (String instance : getRegistry().getAllRemoteInstances()) {
            for (String app : getRegistry().getAllRemoteApps(instance)) {
                Map<String, String> appData = getAppData(instance, app);
                if (matchesQuery(appData, query)) {
                    apps.add(appData);
                }
            }
        }

        return apps;
    }

    private Map<String, String> getAppById(String id) {
        if (id == null) return null;

        String[] parts = id.split("@");
        if (parts.length != 2) return null;

        String appCode = parts[0];
        String instanceId = parts[1];

        if (Constants.LOCAL_INSTANCE_ID.equals(instanceId)) {
            if (getRegistry().getAllLocalApps().contains(appCode)) {
                return getAppData(instanceId, appCode);
            }
        } else {
            if (getRegistry().getAllRemoteInstances().contains(instanceId) &&
                    getRegistry().getAllRemoteApps(instanceId).contains(appCode)) {
                return getAppData(instanceId, appCode);
            }
        }

        return null;
    }

    private Map<String, String> getAppData(String instanceId, String appCode) {
        Map<String, String> app = new HashMap<>();
        app.put("id", appCode + "@" + instanceId);
        app.put("code", appCode);
        app.put("instance", instanceId);

        AppStubLocal appStub = getRegistry().getLocalApp(appCode);
        if (appStub != null) {
            AppMeta appMeta = appStub.getAppMeta();
            if (appMeta != null && appMeta.getApp() != null) {
                qingzhou.dto.meta.annotation.App appInfo = appMeta.getApp();
                app.put("name", getI18nText(appInfo.name));
                app.put("icon", appInfo.icon);
                app.put("info", getI18nText(appInfo.info));
            } else {
                app.put("name", appCode);
                app.put("icon", "");
                app.put("info", "");
            }
        } else {
            app.put("name", appCode);
            app.put("icon", "");
            app.put("info", "");
        }

        if (Constants.LOCAL_INSTANCE_ID.equals(instanceId)) {
            app.put("host", "localhost");
        } else {
            app.put("host", instanceId);
        }

        return app;
    }

    private String getI18nText(String[] texts) {
        if (texts == null || texts.length == 0) return "";
        return texts[0];
    }

    private boolean matchesQuery(Map<String, String> data, Map<String, String> query) {
        if (query == null || query.isEmpty()) return true;

        for (Map.Entry<String, String> entry : query.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                String dataValue = data.get(key);
                if (dataValue == null || !dataValue.toLowerCase().contains(value.toLowerCase())) {
                    return false;
                }
            }
        }

        return true;
    }
}
