package qingzhou.app.master.guide;

import qingzhou.app.master.ReadOnlyDataStore;
import qingzhou.framework.api.DataStore;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(name = "component", icon = "cubes",
        menuName = "Guide", menuOrder = 1,
        nameI18n = {"公共组件", "en:Component"},
        infoI18n = {"展示系统开放给应用的公共组件，公共组件为 Java 语言接口类型，在应用内部可通过轻舟框架提供的 AppContext 接口对象（调用其方法：getService(Class<T> serviceType)）获取具体的组件对象。AppContext 对象可在 QingZhouApp 的 start 方法参数上获得，也可在 Model 内部调用 getAppContext() 方法获得。",
                "en:Displays the public component that the system is open to the application, the public component is the Java language interface type, and the specific component object can be obtained through the AppContext interface object provided by the Qingzhou framework (call its method: getService(Class serviceType)) inside the application<T>. The AppContext object can be obtained on the start method parameter of QingZhouApp, or by calling the getAppContext() method inside the Model."})
public class Component extends ModelBase implements ListModel {
    @ModelField(
            showToList = true,
            nameI18n = {"组件类型", "en:Component Type"},
            infoI18n = {"此公共组件的 Java 语言类型，即类全名。",
                    "en:The Java language type of this public component, which is the full name of the class."})
    public String id;

    @Override
    public DataStore getDataStore() {
        return new ReadOnlyDataStore(type -> {
            List<Map<String, String>> data = new ArrayList<>();
            getAppContext().getServiceTypes().forEach(aClass -> data.add(new HashMap<String, String>() {{
                put("id", aClass.getName());
            }}));
            return data;
        });
    }
}
