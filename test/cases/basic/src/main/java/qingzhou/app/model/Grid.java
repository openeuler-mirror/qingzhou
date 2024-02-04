package qingzhou.app.model;


import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

@Model(name = "Grid", icon = "table",
        nameI18n = {"Grid模块", "en:Grid Test"},
        infoI18n = {"测试增删改查相关功能", "en:Test the functions related to adding, deleting, modifying, and querying"}
)
public class Grid extends ModelBase implements ListModel {

    @Override
    @ModelAction(name = ACTION_NAME_LIST,
            icon = "table", forwardToPage = "grid",
            nameI18n = {"网格", "en:grid"},
            infoI18n = {"grid", "en:grid"})
    public void list(Request request, Response response) throws Exception {
        for (int i = 0; i < 12; i++) {
            response.addDataObject(new Add("data-" + i, "info----" + i));
        }
    }
}
