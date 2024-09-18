package qingzhou.app.model;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.app.AddableModelBase;
import qingzhou.app.ExampleMain;

@Model(code = "post", icon = "stack",
        menu = ExampleMain.SYSTEM_MANAGEMENT, order = 3,
        name = {"岗位", "en:Post"},
        info = {"岗位管理。", "en:Post management."})
public class Post extends AddableModelBase {
    @ModelField(
            required = true,
            list = true,
            name = {"岗位名称", "en:Post Name"},
            info = {"岗位名称。", "en:Post name."})
    public String name;

    @ModelField(
            required = true,
            list = true,
            name = {"岗位编码", "en:Post Code"},
            info = {"岗位编码。", "en:Post code."})
    public String postCode;

    @ModelField(
            type = FieldType.radio,
            options = {"正常", "停用"},
            list = true,
            name = {"岗位状态", "en:Post Status"},
            info = {"岗位状态。", "en:Post status."})
    public String postStatus;

    @ModelField(
            type = FieldType.textarea,
            list = true,
            name = {"备注", "en:Notes"},
            info = {"备注。", "en:Notes."})
    public String notes;

    @Override
    public String idFieldName() {
        return "postCode";
    }
}
