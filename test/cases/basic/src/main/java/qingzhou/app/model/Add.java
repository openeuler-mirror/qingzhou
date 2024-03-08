package qingzhou.app.model;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Createable;

@Model(name = "test", icon = "leaf",
        nameI18n = {"Add测试模块", "en:Add Test"},
        infoI18n = {"测试增删改查相关功能", "en:Test the functions related to adding, deleting, modifying, and querying"}
)
public class Add extends ModelBase implements Createable {

    @ModelField(
            required = true,
            showToList = true,
            disableOnEdit = true,
            nameI18n = {"ID", "en:ID"},
            infoI18n = {"测试Model的ID。", "en:ID of test model"})
    public String id;

    @ModelField(
            required = true,
            showToList = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"名称。", "en:Name"})
    public String name;

    public Add() {
    }

    public Add(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
