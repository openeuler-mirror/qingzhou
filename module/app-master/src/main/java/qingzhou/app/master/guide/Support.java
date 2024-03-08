package qingzhou.app.master.guide;

import qingzhou.api.DataStore;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Listable;
import qingzhou.framework.app.ReadOnlyDataStore;

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
    public DataStore getDataStore() {
        return new ReadOnlyDataStore() {
            @Override
            public List<Map<String, String>> getAllData(String type) {
                return null;
            }
        };
    }
}
