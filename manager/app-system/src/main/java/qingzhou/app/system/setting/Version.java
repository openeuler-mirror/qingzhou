package qingzhou.app.system.setting;

import qingzhou.api.*;
import qingzhou.api.type.Add;
import qingzhou.api.type.Show;
import qingzhou.app.system.Main;
import qingzhou.app.system.service.Instance;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Model(code = "version", icon = "upload-alt",
        menu = Main.SETTING_MENU,
        order = 5,
        name = {"版本", "en:Product Version"},
        info = {"管理轻舟的运行版本，将轻舟升级到一个新的版本。注：升级包会立即下发，但在实例下次重启时生效。",
                "en:Manage the running version of the light boat and upgrade the light boat to a new version. Note: The upgrade package is delivered immediately, but takes effect the next time the instance is restarted."})
public class Version extends ModelBase implements Add, qingzhou.api.type.List, Show {

    @Override
    public String idField() {
        return "version";
    }

    @ModelField(
            createable = false,
            list = true,
            name = {"产品版本", "en:Product Version"},
            info = {"产品的版本号。", "en:Version number of the product."})
    public String version;

    @ModelField(
            createable = false,
            list = true,
            name = {"生效中", "en:Running"},
            info = {"此版本是否处于生效状态。", "en:Whether this version is in effect."})
    public String running;

    @ModelField(
            type = FieldType.bool,
            name = {"使用上传", "en:Enable Upload"},
            info = {"升级包可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The upgrade package can be uploaded from the client or read from a location specified on the server side."})
    public Boolean upload = false;

    @ModelField(
            show = "upload=false",
            required = true,
            filePath = true,
            name = {"文件位置", "en:Path"},
            info = {"服务器上升级包的位置。注：须为 version*.zip 类型的文件。",
                    "en:The location of the upgrade package on the server. Note: It must be a file of type version*.zip."})
    public String path;

    @ModelField(
            show = "upload=true",
            type = FieldType.file,
            required = true,
            name = {"上传文件", "en:File"},
            info = {"上传一个文件到服务器，文件须是 version*.zip 类型的文件，否则可能会导致升级失败。",
                    "en:Upload a file to the server that must be of type version*.zip or the upgrade may fail."})
    public String file;

    @ModelField(
            type = FieldType.markdown,
            createable = false,
            name = {"发布说明", "en:Release Notes"},
            info = {"此版本的说明信息，通常会包括该版本的新增功能、修复已知问题等内容。",
                    "en:The description of this release, which usually includes new features in the release, fixes for known issues, and so on."})
    public String releaseNotes;

    @ModelAction(
            code = DeployerConstants.AGENT_UNINSTALL_VERSION, icon = "trash",
            order = 9,
            batch = true,
            show = "running=false",
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) {
        invokeOnInstances(request);
    }

    @ModelAction(
            code = DeployerConstants.AGENT_INSTALL_VERSION, icon = "plus-sign",
            name = {"升级", "en:Upgrade"},
            info = {"将轻舟升级到一个新的版本。", "en:Upgrade the light boat to a new version."})
    public void add(Request request) {
        invokeOnInstances(request);
    }


    private void invokeOnInstances(Request request) {
        RequestImpl requestImpl = (RequestImpl) request;
        String originModel = request.getModel();
        try {
            requestImpl.setModelName(DeployerConstants.MODEL_AGENT);
            List<Response> responseList = Main.getService(ActionInvoker.class)
                    .invokeOnInstances(request, Instance.allInstanceIds(null));
            final StringBuilder[] error = {null};
            responseList.forEach(response -> {
                if (!response.isSuccess()) {
                    request.getResponse().setSuccess(false);
                    if (error[0] == null) {
                        error[0] = new StringBuilder();
                    }
                    error[0].append(response.getMsg()).append(" ");
                }
            });

            if (!request.getResponse().isSuccess()) {
                String errorMsg = error[0].toString();
                request.getResponse().setMsg(errorMsg);
            }
        } finally {
            requestImpl.setModelName(originModel);
        }
    }

    public String[] allIds(Map<String, String> query) throws Exception {
        if (query != null) {
            List<Map<String, String>> collect = VersionUtil.versionList().stream().filter(map -> {
                for (String key : query.keySet()) {
                    boolean equals = map.get(key).equals(query.get(key));
                    if (!equals) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());
            return collect.stream().map(map -> map.get("version")).toArray(String[]::new);
        } else {
            return VersionUtil.versionList().stream().map(map -> map.get("version")).toArray(String[]::new);
        }
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws Exception {
        if (query != null) {
            return VersionUtil.versionList().stream().filter(map -> {
                for (String key : query.keySet()) {
                    boolean equals = map.get(key).equals(query.get(key));
                    if (!equals) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());
        } else {
            return VersionUtil.versionList();
        }
    }

    @Override
    public Map<String, String> showData(String id) throws Exception {
        final LinkedList<Map<String, String>> maps = VersionUtil.versionList();
        for (Map<String, String> map : maps) {
            if (map.get("version").equals(id)) {
                return map;
            }
        }
        return null;
    }
}
