package qingzhou.app.master.guide;

import qingzhou.framework.api.EditModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

@Model(name = "appdev", icon = "book",
        menuName = "Guide", menuOrder = 1,
        entryAction = EditModel.ACTION_NAME_EDIT,// todo 这个开发页面需要再设计
        nameI18n = {"应用开发", "en:App Dev"},
        infoI18n = {"基于轻舟进行应用开发的具体步骤。", "en:Specific steps for application development based on QingZhou."})
public class AppDev extends ModelBase implements EditModel {
    @ModelField(nameI18n = {"项目", "en:Project"}, showToList = true,
            infoI18n = {"支持项目的名称。", "en:The name of the supporting project."})
    private String id;

    @Override
    public void show(Request request, Response response) {
    }
}
