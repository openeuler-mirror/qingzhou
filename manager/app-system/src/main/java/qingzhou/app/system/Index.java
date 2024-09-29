package qingzhou.app.system;

import qingzhou.api.*;
import qingzhou.api.type.Show;
import qingzhou.deployer.DeployerConstants;

@Model(code = DeployerConstants.MODEL_INDEX, icon = "home",
        entrance = Show.ACTION_SHOW,
        name = {"主页", "en:Home"},
        info = {"查看 QingZhou 平台的相关信息。", "en:Check out the relevant information of QingZhou platform."})
public class Index extends ModelBase {
    @ModelField(
            name = {"平台名称", "en:Platform Name"},
            info = {"QingZhou 平台的名称。", "en:The name of QingZhou platform."})
    public String name;

    @ModelField(
            name = {"平台版本", "en:Platform Version"},
            info = {"QingZhou 平台的版本。", "en:This version of this QingZhou platform."})
    public String version;

    @ModelField(
            name = {"Java 环境", "en:Java Env"},
            info = {"运行 QingZhou 实例的 Java 环境。", "en:The Java environment in which QingZhou instance is running."})
    public String javaHome;

    @ModelAction(
            code = Show.ACTION_SHOW,
            page = "show",
            name = {"主页", "en:Home"},
            info = {"查看 QingZhou 平台的相关信息。",
                    "en:View QingZhou platform information."})
    public void show(Request request) throws Exception {
        Index index = new Index();
        index.name = "QingZhou（轻舟）";
        index.version = getAppContext().getPlatformVersion();
        index.javaHome = System.getProperty("java.home");
        request.getResponse().addModelData(index);
    }

    @ModelAction(// NOTE: 这个方法用作是 Login 成功后 跳过的
            code = DeployerConstants.ACTION_INDEX,
            page = "sys/index",
            name = {"主页", "en:Home"},
            info = {"进入 QingZhou 平台的主页。",
                    "en:View QingZhou platform information."})
    public void index(Request request) throws Exception {
        show(request);
    }
}
