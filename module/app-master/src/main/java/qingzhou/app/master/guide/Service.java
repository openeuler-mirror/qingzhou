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
        nameI18n = {"公共服务", "en:Service"},
        infoI18n = {"展示系统开放给应用的公共服务，公共服务为 Java 语言接口类型，在应用内部可通过轻舟框架提供的 AppContext 接口对象（调用其方法：getService(Class<T> serviceType)）获取具体的服务对象。AppContext 对象可在 QingZhouApp 的 start 方法参数上获得，也可在 Model 内部调用 getAppContext() 方法获得。",
                "en:Displays the public service that the system is open to the application, the public service is the Java language interface type, and the specific service object can be obtained through the AppContext interface object provided by the Qingzhou framework (call its method: getService(Class serviceType)) inside the application<T>. The AppContext object can be obtained on the start method parameter of QingZhouApp, or by calling the getAppContext() method inside the Model."})
public class Service extends ModelBase implements ListModel {
    @ModelField(
            showToList = true,
            nameI18n = {"服务类型", "en:Service Type"},
            infoI18n = {"此公共服务的 Java 语言类型，即类全名。",
                    "en:The Java language type of this public service, which is the full name of the class."})
    public String id;

    @Override
    public DataStore getDataStore() {
        return new ReadOnlyDataStore(type -> {
            List<Map<String, String>> data = new ArrayList<>();
            getAppContext().getServiceTypes().forEach(aClass -> {
                data.add(new HashMap<String, String>() {{
                    put("id", aClass.getName());
                }});
            });
            return data;
        });
    }
}
