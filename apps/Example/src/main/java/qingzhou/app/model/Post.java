package qingzhou.app.model;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;

@Model(code = "post", icon = "stack",
        menu = ExampleMain.MENU_11, order = 1,
        name = {"岗位", "en:Post"},
        info = {"岗位管理。", "en:Post management."})
public class Post extends AddModelBase {
    @ModelField(
            required = true,
            list = true,
            name = {"岗位名称", "en:Post Name"},
            info = {"岗位名称。", "en:Post name."})
    public String name;

    @ModelField(
            required = true,
            list = true,
            name = {"岗位编码", "en:Post Code"})
    public String postCode;

    @ModelField(
            type = FieldType.radio,
            options = {"正常", "停用"},
            color = {"正常:success", "停用:danger"},
            list = true,
            name = {"岗位状态", "en:Post Status"})
    public String postStatus;

    @ModelField(
            type = FieldType.textarea,
            list = true,
            name = {"备注", "en:Notes"})
    public String notes;

    @Override
    public String idField() {
        return "postCode";
    }
}
