package qingzhou.console.master.system;

import qingzhou.api.console.Model;
import qingzhou.api.console.ModelAction;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.model.AddModel;
import qingzhou.console.master.MasterModelBase;

@Model(name = "version", icon = "upload-alt",
        menuName = "System", menuOrder = 1,
        nameI18n = {"版本升级", "en:Version Upgrade"},
        infoI18n = {"管理轻舟产品的各个版本，可将系统整体升级到一个新的版本。注：升级文件会立即下发，对于运行中的节点或实例会在其列表页面给出升级提示，提示重启生效。",
                "en:Manage the various versions of the Qingzhou product and upgrade the system as a whole to a new version. Note: The upgrade file will be issued immediately, and an upgrade prompt will be given on the list page of the running node or instance, indicating that the restart takes effect."})
public class Version extends MasterModelBase implements AddModel {
    @ModelField(
            required = true, unique = true, showToList = true,
            nameI18n = {"ID", "en:ID"},
            infoI18n = {"ID。", "en:ID."})
    public String id;

    @Override
    @ModelAction(name = ACTION_NAME_CREATE,
            icon = "plus-sign", forwardToPage = "form",
            nameI18n = {"升级", "en:Upgrade"},
            infoI18n = {"上传产品新版本的升级包，将系统整体升级到一个新的版本。",
                    "en:Upload the upgrade package of the new version of the product to upgrade the system as a whole to a new version."})
    public void create(Request request, Response response) throws Exception {
        AddModel.super.create(request, response);
    }
}
