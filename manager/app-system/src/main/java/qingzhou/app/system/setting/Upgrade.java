package qingzhou.app.system.setting;

import qingzhou.api.*;
import qingzhou.api.type.Add;
import qingzhou.api.type.Delete;
import qingzhou.api.type.Show;
import qingzhou.app.system.Main;
import qingzhou.app.system.VersionUtil;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import java.util.*;
import java.util.stream.Collectors;

@Model(
        code = "upgrade",
        icon = "upload-alt",
        menu = Main.SETTING_MENU,
        order = 6,
        name = {"产品升级", "en:Product Upgrade"},
        info = {"将轻舟升级到一个新的版本。注：升级文件会立即下发，重启生效。",
                "en:Upgrade the light boat to a new version. Note: The upgrade file will be issued immediately and will take effect upon restart."})
public class Upgrade extends ModelBase implements Add, Delete, qingzhou.api.type.List, Show {

    @Override
    public String idField() {
        return "version";
    }

    @ModelField(
            createable = false, editable = false,
            list = true,
            name = {"产品版本", "en:product version"},
            info = {"产品的版本号。", "en:Version number of the product."})
    public String version;

    @ModelField(
            createable = false, editable = false,
            list = true,
            name = {"生效中", "en:Is it in an effective state"},
            info = {"是否处于生效状态", "en:Is it in an effective state."})
    public String running;

    @ModelField(
            type = FieldType.bool,
            editable = false,
            name = {"使用上传", "en:Enable Upload"},
            info = {"升级包可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The upgrade package can be uploaded from the client or read from a location specified on the server side."})
    public Boolean upload = false;

    @ModelField(
            show = "upload=false",
            required = true,
            filePath = true,
            name = {"应用位置", "en:Application File"},
            info = {"服务器上升级包的位置，通常是应用的程序包，注：须为*.zip 类型的文件。",
                    "en:The location of the upgrade package on the server is usually the application package, note: it must be a *. zip file."})
    public String path;

    @ModelField(
            show = "upload=true",
            type = FieldType.file,
            required = true,
            name = {"上传应用", "en:Upload Application"},
            info = {"上传一个应用文件到服务器，文件须是*.zip 类型的文件，否则可能会导致升级失败。",
                    "en:Upload an application file to the server, which must be a *. zip file type, otherwise it may cause upgrade failure."})
    public String file;
    @ModelField(
            type = FieldType.markdown,
            createable = false,
            name = {"发布说明", "en:Release Notes"},
            info = {"版本升级的说明信息，通常会包括该版本的新增功能、修复已知问题等内容。",
                    "en:Description of a version upgrade, which typically includes new features in that release, fixes for known issues, and so on."})
    public String releaseNotes;

    @ModelAction(
            code = Delete.ACTION_DELETE, icon = "trash",
            order = 9,
            batch = true,
            show = "running=false",
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) throws Exception {
        invokeOnInstances(request);
    }

    @ModelAction(
            code = Add.ACTION_ADD, icon = "plus-sign",
            name = {"升级", "en:upgrade"},
            info = {"指定一个可升级的包进行升级",
                    "en:Specify an upgradable package for upgrading."})
    public void add(Request request) {
        invokeOnInstances(request);
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


    private void invokeOnInstances(Request request) {
        RequestImpl requestImpl = (RequestImpl) request;
        String originModel = request.getModel();
        try {
            requestImpl.setModelName(DeployerConstants.MODEL_UPGRADE_AGENT);
            List<Response> responseList = Main.getService(ActionInvoker.class)
                    .invokeOnInstances(request);
            final StringBuilder[] error = {null};
            responseList.forEach(response -> {
                if (!response.isSuccess()) {
                    request.getResponse().setSuccess(false);
                    if (error[0] == null) {
                        error[0] = new StringBuilder();
                    }
                    error[0].append(response.getMsg());
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

    @Override
    public void addData(Map<String, String> data) throws Exception {

    }

    @Override
    public void deleteData(String id) throws Exception {

    }
}
