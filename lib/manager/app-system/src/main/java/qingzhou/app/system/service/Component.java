package qingzhou.app.system.service;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.engine.Service;
import qingzhou.engine.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(code = Component.MODEL_NAME, icon = "cubes",
        menu = Main.Service,
        order = "1",
        name = {"公共组件", "en:Component"},
        info = {"轻舟为应用提供了一系列开箱即用的公共组件，包括简单日志服务、Java 序列化、二维码生成、Json 文件处理、加解密服务、Http 工具等，以提高业务系统的开发效率。使用轻舟的公共组件非常方便，只需调用 AppContext 对象的 getService 方法传入组件类型，即可获得相应的组件对象。",
                "en:Qingzhou provides a series of out-of-the-box public components for applications, including simple log service, Java serialization, QR code generation, Json file processing, encryption and decryption services, Http tools, etc., to improve the development efficiency of business systems. It is very convenient to use the public component of Qingzhou, just call the getService method of the AppContext object to pass in the component type, and the corresponding component object can be obtained."}
)
public class Component extends ModelBase implements qingzhou.api.type.List {
    public static final String MODEL_NAME = "component";

    @ModelField(hidden = true,
            id = true,
            name = {"ID", "en:ID"})
    public String id;

    @ModelField(
            list = true, search = true,
            width_percent = 20,
            name = {"组件名称", "en:Component Name"},
            info = {"组件名称", "en:Component Name"})
    public String name;

    @ModelField(
            list = true, search = true,
            width_percent = 30,
            name = {"组件类型", "en:Component Type"},
            info = {"组件类型", "en:Component type"})
    public String type;

    @ModelField(
            list = true, search = true,
            width_percent = 35,
            name = {"描述信息", "en:Component Info"},
            info = {"该公共组件的描述信息。", "en:A description of the common component."})
    public String info;

    @ModelField(
            list = true, search = true,
            width_percent = 15,
            color = {"true:Green", "false:Gray"},
            name = {"使用中", "en:In Use"},
            info = {"表示该组件的使用状态。", "en:Indicates the usage status of the component."})
    public boolean inUse;

    @Override
    public boolean contains(String id) {
        String[] ids = allIds(null);
        for (String s : ids) {
            if (s.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private String[] allIds(Map<String, String> query) {
        return getAppContext().getServiceTypes().stream().filter(aClass -> ModelUtil.query(query, new ModelUtil.Supplier() {
            @Override
            public String getModelName() {
                return MODEL_NAME;
            }

            @Override
            public Map<String, String> get() {
                return showData(getServiceId(aClass));
            }
        })).map(Class::getName).toArray(String[]::new);
    }

    private String getServiceId(Class<?> clazz) {
        return clazz.getName();
    }

    public Map<String, String> showData(String id) {
        for (Class<?> serviceType : getAppContext().getServiceTypes()) {
            if (!getServiceId(serviceType).equals(id)) continue;

            String name = null;
            String info = null;
            Service annotation = serviceType.getAnnotation(Service.class);
            if (annotation != null) {
                name = annotation.name();
                info = annotation.description();
            }
            if (Utils.isBlank(name)) {
                name = serviceType.getSimpleName();
            }
            String finalName = name;
            String finalInfo = info;
            return new HashMap<String, String>() {{
                put("id", id);
                put("name", finalName);
                put("type", serviceType.getName());
                put("info", finalInfo);
                put("inUse", "false"); // todo 模拟数据
            }};
        }

        return null;
    }

    @Override
    public List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws IOException {
        return ModelUtil.listData(allIds(query), this::showData, pageNum, pageSize, showFields);
    }

    @Override
    public int pageSize() {
        return Integer.MAX_VALUE;
    }
}
