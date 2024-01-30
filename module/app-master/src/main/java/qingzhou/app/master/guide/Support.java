package qingzhou.app.master.guide;

import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;

@Model(name = "support", icon = "bookmark-empty",
        menuName = "Guide", menuOrder = 2,
        nameI18n = {"支持项目", "en:Support Project"},
        infoI18n = {"列出轻舟可以支持的项目。", "en:Make a list of the projects that QingZhou can support."})
public class Support extends ModelBase implements ListModel {
    @ModelField(nameI18n = {"项目", "en:Project"}, showToList = true,
            infoI18n = {"支持项目的名称。", "en:The name of the supporting project."})
    private String id;

    @ModelField(nameI18n = {"概要", "en:Summary"}, showToList = true,
            infoI18n = {"项目的概要信息。", "en:Brief information about the project."})
    private String summary;

    @ModelField(nameI18n = {"使用说明", "en:Usage"},
            infoI18n = {"轻舟支持该项目的具体方式。", "en:The specific ways in which QingZhou supports the project."})
    private String usage;

    private String downloadName;
}
