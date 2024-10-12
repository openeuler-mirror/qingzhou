package qingzhou.app.system.service;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.registry.AppInfo;
import qingzhou.registry.ModelFieldInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(code = "component", icon = "bullseye",
        menu = Main.Service,
        order = 1,
        name = {"公共组件", "en:Component"},
        info = {"轻舟为应用提供了一系列开箱即用的公共组件，包括加密服务、JSON 文件处理、Servlet 服务、SSH 服务和二维码生成器等，以提高业务系统的开发效率。使用轻舟的公共组件非常方便，只需调用 AppContext 对象的 getService 方法传入组件类型，即可获得相应的组件对象。",
                "en:Qingzhou provides a series of out-of-the-box public components for applications, including encryption services, JSON file processing, servlet services, SSH services, and QR code generators, etc., to improve the development efficiency of business systems. It is very convenient to use the public component of Qingzhou, just call the getService method of the AppContext object to pass in the component type, and the corresponding component object can be obtained."}
)
public class Component extends ModelBase implements qingzhou.api.type.List {
    @ModelField(
            list = true, search = true,
            widthPercent = 30,
            name = {"组件名称", "en:Component Name"},
            info = {"组件名称", "en:Component Name"})
    public String id;

    @ModelField(
            list = true, search = true,
            widthPercent = 70,
            name = {"组件类型", "en:Component Type"},
            info = {"组件类型", "en:Component type"})
    public String type;

    @Override
    public String[] allIds(Map<String, String> query) {
        List<String> ids = new ArrayList<>();
        for (Class<?> serviceType : getAppContext().getServiceTypes()) {
            ids.add(serviceType.getSimpleName());
        }
        List<String> result = new ArrayList<>(ids);
        result.removeIf(id -> !ModelUtil.query(query, new ModelUtil.Supplier() {
            @Override
            public String getFieldSeparator(String field) {
                AppInfo appInfo = Main.getService(Deployer.class).getApp(DeployerConstants.APP_SYSTEM).getAppInfo();
                ModelFieldInfo fieldInfo = appInfo.getModelInfo("component").getModelFieldInfo(field);
                return fieldInfo.getSeparator();
            }

            @Override
            public Map<String, String> get() {
                return showData(id);
            }
        }));
        result.sort(String::compareTo);
        return result.toArray(new String[0]);
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws Exception {
        return ModelUtil.listData(allIds(query), this::showData, pageNum, pageSize, showFields);
    }

    public Map<String, String> showData(String id) {
        for (Class<?> serviceType : getAppContext().getServiceTypes()) {
            if (id.equals(serviceType.getSimpleName())) {
                return new HashMap<String, String>() {{
                    put(idField(), serviceType.getSimpleName());
                    put("type", serviceType.getName());
                }};
            }
        }
        return null;
    }
}
