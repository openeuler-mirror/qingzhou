package qingzhou.app.master.guide;

import qingzhou.api.*;
import qingzhou.api.type.Listable;
import qingzhou.app.master.ReadOnlyDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model(name = "support", icon = "bookmark-empty",
        menuName = "Guide", menuOrder = 2,
        nameI18n = {"支持项目", "en:Support Project"},
        infoI18n = {" Qingzhou 可以支持的项目，如系统集成方面等。", "en:Qingzhou can support projects, such as system integration, etc."})
public class Support extends ModelBase implements Listable {
    @ModelField(
            showToList = true,
            nameI18n = {"项目", "en:Project"},
            infoI18n = {"支持项目的名称。", "en:The name of the supporting project."})
    public String id;

    @ModelField(
            showToList = true,
            nameI18n = {"概要", "en:Summary"},
            infoI18n = {"项目的概要信息。", "en:Brief information about the project."})
    public String summary;

    @ModelField(
            nameI18n = {"使用说明", "en:Usage"},
            infoI18n = {" Qingzhou 支持该项目的具体方式。", "en:The specific ways in which Qingzhou supports the project."})
    public String usage;

    @Override
    @ModelAction(name = ACTION_NAME_SHOW,
            showToList = true,
            icon = "info-sign", forwardToPage = "show",
            nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        super.show(request, response);
    }

    @Override
    public DataStore getDataStore() {
        return new ReadOnlyDataStore(type -> {
            List<Map<String, String>> data = new ArrayList<>();

            // todo: 这里可以参考： Manual.getDataStore(xxx)
//            for (int i = 1; i <= 3; i++) { // 需要和 init 里面的 I18n 的序号保持一致
//                Map<String, String> model = new HashMap<>();
//                model.put("id", "manual-" + i);
//                model.put("name", context.getI18n(getI18nLang(), "manual.name." + i));
//                model.put("info", context.getI18n(getI18nLang(), "manual.info." + i));
//                data.add(model);
//            }
            return data;
        });
    }
}
