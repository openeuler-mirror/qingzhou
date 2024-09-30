package qingzhou.app.system.setting;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;


import java.util.*;

@Model(code = "component", icon = "branch",
        menu = Main.SETTING_MENU,
        order = 7,
        name = {"公共组件", "en:Common Component"},
        info = {"轻舟为应用提供了一系列公共组件，包括加密服务、JSON 文件处理、Servlet 服务、SSH 服务和二维码生成器等，以支持各种功能和服务。在使用这些公共组件时，需要调用appContext.getService(xxx.class)，其中参数为对应的公共组件类，以获取相应的组件对象。",
                "en:Qingzhou provides a series of public components for applications, including encryption services, JSON file processing, servlet services, SSH services, " +
                        "and QR code generators, to support various functions and services. When using these public components, you need to call appContext.getService(xxx.class), where the parameters are the corresponding public component classes, to get the corresponding component objects."}
)
public class Component extends ModelBase implements qingzhou.api.type.List {
    @ModelField(
            list = true,
            name = {"组件名称", "en:Component id"},
            info = {"组件名称", "en:Component id"})
    public String id;

    @ModelField(
            list = true,
            name = {"组件类型", "en:Component Type"},
            info = {"组件类型", "en:Component type"})
    public String componentType;

    @Override
    public String[] allIds(Map<String, String> query) {
        List<String> ids = new ArrayList<>();
        for (Class<?> serviceType : getAppContext().getServiceTypes()) {
            ids.add(serviceType.getSimpleName());
        }
        List<String> result = new ArrayList<>(ids);
        result.removeIf(id -> !ModelUtil.query(query, () -> showData(id)));
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
                    put("componentType", serviceType.getName());
                }};
            }
        }
        return null;
    }
}
