package qingzhou.console.master.service;

import qingzhou.api.console.Model;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.model.AddModel;
import qingzhou.console.master.MasterModelBase;
import qingzhou.console.util.Constants;

@Model(name = Constants.MODEL_NAME_sshkey, icon = "key",
        menuName = "Service", menuOrder = 4,
        nameI18n = {"SSH密钥", "en:SSH Key"},
        infoI18n = {"管理访问远程节点的SSH密钥。",
                "en:Manage SSH keys to access remote nodes."})
public class SSHKey extends MasterModelBase implements AddModel {
    @ModelField(
            required = true, unique = true, showToList = true,
            nameI18n = {"ID", "en:ID"},
            infoI18n = {"ID。", "en:ID."})
    public String id;
}
