package qingzhou.master.support;

import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.ListModel;
import qingzhou.master.MasterModelBase;

@Model(name = "component", icon = "th",
        menuName = "Support", menuOrder = 2,
        nameI18n = {"组件清单", "en:Component List"},
        infoI18n = {"列出轻舟平台提供的公共组件清单。", "en:List the public components provided by the Qingzhou platform."})
public class Component extends MasterModelBase implements ListModel {
    @ModelField(
            required = true, unique = true, showToList = true,
            nameI18n = {"ID", "en:ID"},
            infoI18n = {"ID。", "en:ID."})
    public String id;
}