package qingzhou.app.model;

import qingzhou.api.InputType;
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
            widthPercent = 10,
            search = true,
            name = {"岗位编码", "en:Post Code"})
    public String postCode;

    @ModelField(
            required = true, linkShow = true,
            ignore = 15,
            search = true, list = true,
            name = {"岗位名称", "en:Post Name"},
            info = {"岗位名称。", "en:Post name."})
    public String name;

    @ModelField(
            inputType = InputType.bool,
            list = true, search = true,
            update = true,
            name = {"岗位状态", "en:Post Status"})
    public String postStatus;

    @ModelField(
            inputType = InputType.textarea,
            ignore = 15,
            update = true,
            list = true, search = true,
            name = {"备注", "en:Notes"})
    public String notes;

    @Override
    public String idField() {
        return "postCode";
    }

    @Override
    public boolean showIdField() {
        return false;
    }

}
