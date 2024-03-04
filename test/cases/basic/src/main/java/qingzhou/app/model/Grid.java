package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.Listable;
import qingzhou.app.MemoryDataStore;

@Model(name = "Grid", icon = "table",
        nameI18n = {"Grid模块", "en:Grid Test"},
        infoI18n = {"测试增删改查相关功能", "en:Test the functions related to adding, deleting, modifying, and querying"}
)
public class Grid extends ModelBase implements Listable {

    @ModelAction(name = Listable.ACTION_NAME_LIST,
            icon = "table", forwardToPage = "grid",
            nameI18n = {"网格", "en:grid"},
            infoI18n = {"grid", "en:grid"})
    public void list(Request request, Response response) {
    }

    @Override
    public DataStore getDataStore() {
        return new MemoryDataStore();
    }
}
