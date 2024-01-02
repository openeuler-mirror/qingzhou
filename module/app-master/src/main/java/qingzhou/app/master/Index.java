package qingzhou.app.master;

import qingzhou.framework.api.*;

import java.util.Map;

@Model(name = "index", icon = "home", entryAction = "home",
        nameI18n = {"主页", "en:Main"},
        infoI18n = {"查看当前轻舟实例的产品、授权等信息。",
                "en:View the product, license, and other information of the current QingZhou instance."})
public class Index extends MasterModelBase {
    @ModelField(
            group = "product",
            nameI18n = {"产品信息", "en:Server Info"},
            infoI18n = {"当前轻舟产品的版本等信息。",
                    "en:Information such as the version of the current QingZhou product."})
    public String serverInfo;

    @ModelField(
            group = "product",
            nameI18n = {"实例目录", "en:Domain Path"},
            infoI18n = {"当前运行的轻舟实例所对应的文件路径。", "en:The file path corresponding to the currently running QingZhou instance."})
    public String domainPath;

    @ModelField(
            group = "product",
            nameI18n = {"Java 环境", "en:Java Env"},
            infoI18n = {"当前运行的轻舟实例所使用的 Java 环境。", "en:The Java environment used by the currently running QingZhou instance."})
    public String javaHome;

    @ModelAction(name = "home",
            icon = "home", forwardToPage = "home",
            nameI18n = {"首页", "en:Home"},
            infoI18n = {"查看 QingZhou 应用服务器的产品和授权信息。",
                    "en:View product and licensing information for QingZhou Application Server."})
    public void home(Request request, Response response) throws Exception {
        Index index = new Index();
        index.serverInfo = "QingZhou（轻舟）";
        index.domainPath = getAppContext().getDomain().getPath();
        index.javaHome = "";// TODO JDKUtil.getJavaHome();
        response.addDataObject(index);
    }

    // 这个方法用作是 Login 成功后 跳过的
    @ModelAction(name = "index",
            icon = "resize", forwardToPage = "index",
            nameI18n = {"首页", "en:Home"},
            infoI18n = {"查看 QingZhou 应用服务器的产品和授权信息。",
                    "en:View product and licensing information for QingZhou Application Server."})
    public void index(Request request, Response response) throws Exception {
        home(request, response);
    }

    @Override
    public void show(Request request, Response response) throws Exception {
        home(request, response);
    }

    @Override
    public Map<String, String> showInternal(Request request) throws Exception {
        return null;
    }
}
