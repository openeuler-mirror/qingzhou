package qingzhou.console.master.product;

import qingzhou.api.console.Model;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.model.AddModel;
import qingzhou.console.master.MasterModelBase;
import qingzhou.console.util.Constants;

@Model(name = Constants.MODEL_NAME_backup, icon = "tags",
        menuName = "Product", menuOrder = 2,
        nameI18n = {"备份", "en:Backup"},
        infoI18n = {"将稳定运行的应用实例或集群配置备份下来，以在未来用于故障恢复或问题排查。备份的配置也可用作创建应用实例的默认配置，以便快速完成应用实例的初始化操作。",
                "en:Back up the stable running application instance or cluster configuration for future failure recovery or troubleshooting. The backed-up configuration can also be used as the default configuration for creating an application instance to quickly complete the initialization of the application instance."})
public class Backup extends MasterModelBase implements AddModel {
    @ModelField(
            required = true, unique = true, showToList = true,
            nameI18n = {"ID", "en:ID"},
            infoI18n = {"ID。", "en:ID."})
    public String id;
}
