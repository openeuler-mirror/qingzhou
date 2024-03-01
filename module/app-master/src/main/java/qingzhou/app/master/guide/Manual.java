package qingzhou.app.master.guide;

import qingzhou.api.*;
import qingzhou.app.master.ReadOnlyDataStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(name = "manual", icon = "book",
        menuName = "Guide", menuOrder = 1,
        nameI18n = {"开发手册", "en:DevManual"},
        infoI18n = {"基于 Qingzhou 进行应用开发的具体操作说明。", "en:Specific operating instructions for application development based on Qingzhou."})
public class Manual extends ModelBase implements ShowModel {
    @ModelField(nameI18n = {"内容", "en:Content"},
            type = FieldType.markdown,
            infoI18n = {"此手册的内容。", "en:The content of the manual."})
    public String content;

    @Override
    public DataStore getDataStore() {
        return new ReadOnlyDataStore(type -> {
            List<Map<String, String>> data = new ArrayList<>();
            Map<String, String> model = new HashMap<>();
            model.put("content", "xxxxxxxxx");
            data.add(model);
            return data;
        });
    }
}
