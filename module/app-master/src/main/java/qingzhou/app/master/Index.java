package qingzhou.app.master;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.api.ShowModel;

@Model(name = FrameworkContext.SYS_MODEL_INDEX, icon = "home",
        entryAction = ShowModel.ACTION_NAME_SHOW,
        nameI18n = {"主页", "en:Main"},
        infoI18n = {"查看轻舟产品的相关信息。", "en:Check out the relevant information of Qingzhou products."})
public class Index extends ModelBase {
    @ModelField(
            group = "product",
            nameI18n = {"产品信息", "en:Server Info"},
            infoI18n = {"当前轻舟产品的版本等信息。",
                    "en:Information such as the version of the current QingZhou."})
    public String info;

    @ModelField(
            group = "product",
            nameI18n = {"安装目录", "en:Domain Path"},
            infoI18n = {"表示轻舟的安装目录。", "en:Indicates the installation directory of QingZhou."})
    public String path;

    @Override
    public void show(Request request, Response response) throws Exception {
        Index index = new Index();
        index.info = "QingZhou（轻舟）";
        index.path = getAppContext().getHome().getPath();
        response.addDataObject(index);
    }

    @ModelAction(name = FrameworkContext.SYS_MODEL_INDEX, // NOTE: 这个方法用作是 Login 成功后 跳过的
            icon = "resize",
            forwardToPage = "index",
            nameI18n = {"首页", "en:Home"},
            infoI18n = {"查看轻舟的产品和授权信息。",
                    "en:View Qingzhou product and licensing information."})
    public void index(Request request, Response response) throws Exception {
        show(request, response);
    }
}
