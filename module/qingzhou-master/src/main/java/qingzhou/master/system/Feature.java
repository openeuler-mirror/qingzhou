package qingzhou.master.system;

import qingzhou.framework.api.Model;
import qingzhou.framework.api.EditModel;
import qingzhou.master.MasterModelBase;

@Model(name = "feature", icon = "window-alt",
        menuName = "System", menuOrder = 3,
        nameI18n = {"功能定制", "en:Feature Customization"},
        infoI18n = {"定制可管理的功能项，关闭一些不常用的功能，可使得系统更加轻盈，更便于使用，同时可以规避一些安全隐患。",
                "en:Customizing manageable functions and turning off some unused functions can make the system lighter and easier to use, while avoiding some security risks."})
public class Feature extends MasterModelBase implements EditModel {

}
