package qingzhou.app.model;

import qingzhou.api.FieldType;
import qingzhou.api.Item;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.Option;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;

@Model(code = "post", icon = "stack",
        menu = ExampleMain.MENU_11, order = 1,
        name = {"岗位", "en:Post"},
        info = {"岗位管理。", "en:Post management."})
public class Post extends AddModelBase implements Option {
    @ModelField(
            required = true,
            list = true, search = true,
            name = {"岗位名称", "en:Post Name"},
            info = {"岗位名称。", "en:Post name."})
    public String name;

    @ModelField(
            required = true,
            list = true, search = true,
            name = {"岗位编码", "en:Post Code"})
    public String postCode;

    @ModelField(
            type = FieldType.radio,
            color = {"yes:Green", "no:Red"},
            list = true, search = true,
            widthPercent = 50,
            name = {"岗位状态", "en:Post Status"})
    public String postStatus;

    @ModelField(
            type = FieldType.textarea,
            color = {"12345678901234456789:Green", "00:Red"},
            list = true, search = true,
            name = {"备注", "en:Notes"})
    public String notes;

    @Override
    public String idField() {
        return "postCode";
    }

    @Override
    public Item[] optionData(String fieldName) {
        if (fieldName.equals("postStatus")) {
            return new Item[]{
                    Item.of("yes", new String[]{"正常", "en:Normal"}),
                    Item.of("no", new String[]{"停用", "en:Deactivated"}),
            };
        }
        return null;
    }
}
