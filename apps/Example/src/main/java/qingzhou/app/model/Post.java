package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.Option;
import qingzhou.app.AddModelBase;
import qingzhou.app.ExampleMain;

@Model(code = "post", icon = "stack",
        menu = ExampleMain.MENU_1, order = "3",
        name = {"岗位", "en:Post"},
        info = {"岗位管理。", "en:Post management."})
public class Post extends AddModelBase implements Option {
    public Post() {
        super("id");
    }

    @ModelField(
            required = true,
            show = false,
            width_percent = 10,
            id = true,
            search = true,
            name = {"岗位编码", "en:Post Code"})
    public String postCode;

    @ModelField(
            required = true, link_action = "show",
            ignore = 15,
            search = true, list = true,
            name = {"岗位名称", "en:Post Name"},
            info = {"岗位名称。", "en:Post name."})
    public String name;

    @ModelField(
            input_type = InputType.grouped_multiselect,
            list = true, search = true, static_option = true,
            update_action = "listUpdate",
            name = {"分组多选下拉", "en:"})
    public String groupedMultiselect;

    @ModelField(
            input_type = InputType.bool,
            list = true, search = true,
            update_action = "listUpdate",
            name = {"岗位状态", "en:Post Status"})
    public String postStatus;


    @ModelField(
            input_type = InputType.textarea,
            ignore = 15,
            update_action = "listUpdate",
            list = true, search = true,
            name = {"备注", "en:Notes"})
    public String notes;

    @ModelAction(
            name = {}, code = "listUpdate")
    public void show(Request request) throws Exception {
        request.getResponse().setMsg("岗位更新成功");
    }

    @Override
    public Item[] optionData(String id, String fieldName) {
        if (fieldName.equals("groupedMultiselect")) {
            return new Item[]{
                    Item.of("1", new String[]{"开发分组", "en:Dev"}),
                    Item.of("1_001", new String[]{"开发", "en:Dev"}),
                    Item.of("2", new String[]{"测试分组", "en:Test"}),
                    Item.of("2_002", new String[]{"测试", "en:Test"}),
                    Item.of("3", new String[]{"运维分组", "en:ops"}),
                    Item.of("3_003", new String[]{"运维", "en:ops"}),
            };
        }
        return new Item[0];
    }
}
