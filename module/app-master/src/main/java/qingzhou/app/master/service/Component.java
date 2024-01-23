package qingzhou.app.master.service;

import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;

@Model(name = "component", icon = "cubes",
        menuName = "Service", menuOrder = 3,
        nameI18n = {"组件", "en:Component"},
        infoI18n = {"展示系统开放给应用的公共组件服务。",
                "en:Displays the public component services that the system is open to applications."})
public class Component extends ModelBase implements ListModel { // todo：列出 AppContext 的 getServiceTypes()
}
