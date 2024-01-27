package qingzhou.app.model;

import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;

@Model(name = "test", icon = "leaf",
        nameI18n = {"测试", "en:Test"},
        infoI18n = {"测试用的Model", "en:Test Model"}
)
public class Test extends ModelBase implements ListModel {

    @ModelField(
            required = true,
            showToList = true,
            disableOnEdit = true,
            nameI18n = {"ID", "en:ID"},
            infoI18n = {"测试Model的ID。", "en:ID of test model"})
    public String id;
}
