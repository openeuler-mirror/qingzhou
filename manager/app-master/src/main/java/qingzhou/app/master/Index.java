package qingzhou.app.master;

import qingzhou.api.*;
import qingzhou.engine.ModuleContext;

@Model(code = "index", icon = "home",
        entrance = "show",
        name = {"主页", "en:Home"},
        info = {"查看 QingZhou 产品的相关信息。", "en:Check out the relevant information of QingZhou products."})
public class Index extends ModelBase {
    @ModelField(
            name = {"产品名称", "en:Product Name"},
            info = {"QingZhou 平台的名称。", "en:The name of QingZhou platform."})
    public String name;

    @ModelField(
            name = {"产品版本", "en:Product Version"},
            info = {"QingZhou 平台的版本。", "en:This version of this QingZhou platform."})
    public String version;

    @ModelAction(
            code = "show",
            name = {"主页", "en:Home"},
            info = {"查看 QingZhou 的产品信息。",
                    "en:View QingZhou product information."})
    public void show(Request request) throws Exception {
        Index index = new Index();
        index.name = "QingZhou（轻舟）";
        String versionFlag = "version";
        index.version = MasterApp.getService(ModuleContext.class).getLibDir().getName().substring(versionFlag.length());
        request.getResponse().addModelData(index);
    }

    @ModelAction(// NOTE: 这个方法用作是 Login 成功后 跳过的
            code = "index",
            name = {"主页", "en:Home"},
            info = {"查看 QingZhou 的产品信息。",
                    "en:View QingZhou product information."})
    public void index(Request request) throws Exception {
        show(request);
    }
}
