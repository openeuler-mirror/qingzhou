package qingzhou.app.master;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Showable;
import qingzhou.engine.ModuleContext;

@Model(code = "index", icon = "home",
        entrance = Showable.ACTION_NAME_SHOW,
        name = {"主页", "en:Home"},
        info = {"查看 Qingzhou 产品的相关信息。", "en:Check out the relevant information of Qingzhou products."})
public class Index extends ModelBase implements Showable {
    @ModelField(
            name = {"产品名称", "en:Product Name"},
            info = {"此 Qingzhou 平台的名称。", "en:The name of  this Qingzhou platform."})
    public String name;

    @ModelField(
            name = {"产品版本", "en:Product Version"},
            info = {"此 Qingzhou 平台的版本。", "en:This version of this Qingzhou platform."})
    public String version;

    @ModelAction(
            name = {"主页", "en:Home"},
            info = {"查看 Qingzhou 的产品信息。",
                    "en:View Qingzhou product information."})
    public void show(Request request, Response response) throws Exception {
        Index index = new Index();
        index.name = "Qingzhou（轻舟）";
        String versionFlag = "version";
        index.version = MasterApp.getService(ModuleContext.class).getLibDir().getName().substring(versionFlag.length());
        response.addModelData(index);
    }

    @ModelAction(// NOTE: 这个方法用作是 Login 成功后 跳过的
            name = {"主页", "en:Home"},
            info = {"查看 Qingzhou 的产品信息。",
                    "en:View Qingzhou product information."})
    public void index(Request request, Response response) throws Exception {
        show(request, response);
    }
}
