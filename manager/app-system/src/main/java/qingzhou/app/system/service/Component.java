package qingzhou.app.system.service;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.app.system.Main;
import qingzhou.engine.ServiceInfo;
import qingzhou.engine.util.Utils;

import java.util.ArrayList;
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
            search = true,
            width_percent = 30,
            name = {"组件名称", "en:Component Name"},
            info = {"组件名称", "en:Component Name"})
    public String id;

    @ModelField(
            list = true, search = true,
            width_percent = 70,
            name = {"组件类型", "en:Component Type"},
            info = {"组件类型", "en:Component type"})
    public String type;

    @ModelField(
            list = true, search = true,
            width_percent = 70,
            name = {"描述信息", "en:Component Info"},
            info = {"该公共组件的描述信息。", "en:A description of the common component."})
    public String info;

    @Override
    public List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws Exception {
        List<String[]> list = new ArrayList<>();
        getAppContext().getServiceTypes().forEach(aClass -> {
            Object service = getAppContext().getService(aClass);
            String id = null;
            String info = null;
            if (service instanceof ServiceInfo) {
                ServiceInfo serviceInfo = (ServiceInfo) service;
                id = serviceInfo.getName();
                info = serviceInfo.getDescription();
            }
            if (Utils.isBlank(id)) id = aClass.getSimpleName();
            if (Utils.isBlank(info)) info = aClass.getName();

            list.add(new String[]{id, aClass.getName(), info});
        });
        return list;
    }

    @Override
    public int pageSize() {
        return Integer.MAX_VALUE;
    }
}
