package qingzhou.app.master;

import qingzhou.api.*;
import qingzhou.api.type.Showable;
import qingzhou.framework.app.App;

@Model(name = App.SYS_MODEL_INDEX, icon = "home",
        entryAction = Showable.ACTION_NAME_SHOW,
        nameI18n = {"主页", "en:Home"},
        infoI18n = {"查看 Qingzhou 产品的相关信息。", "en:Check out the relevant information of Qingzhou products."})
public class Index extends ModelBase {
    @ModelField(
            nameI18n = {"产品名称", "en:Product Name"},
            infoI18n = {"此 Qingzhou 平台的名称。", "en:The name of  this Qingzhou platform."})
    public String name;

    @ModelField(
            nameI18n = {"产品版本", "en:Product Version"},
            infoI18n = {"此 Qingzhou 平台的版本。", "en:This version of this Qingzhou platform."})
    public String version;

    @ModelAction(name = ACTION_NAME_SHOW,
            forwardToPage = "show",
            nameI18n = {"主页", "en:Home"},
            infoI18n = {"查看 Qingzhou 的产品信息。",
                    "en:View Qingzhou product information."})
    public void show(Request request, Response response) throws Exception {
        Index index = new Index();
        index.name = Main.getFramework().getName();
        index.version = Main.getFramework().getVersion();
        response.addModelData(index);
    }

    @ModelAction(name = App.SYS_MODEL_INDEX, // NOTE: 这个方法用作是 Login 成功后 跳过的
            forwardToPage = "sys/index",
            nameI18n = {"主页", "en:Home"},
            infoI18n = {"查看 Qingzhou 的产品信息。",
                    "en:View Qingzhou product information."})
    public void index(Request request, Response response) throws Exception {
        show(request, response);
    }
}
