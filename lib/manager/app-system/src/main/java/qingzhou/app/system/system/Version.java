package qingzhou.app.system.system;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import qingzhou.api.ActionType;
import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Add;
import qingzhou.api.type.Delete;
import qingzhou.api.type.Show;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.app.system.business.Instance;
import qingzhou.core.DeployerConstants;
import qingzhou.engine.util.FileUtil;

@Model(code = "version", icon = "upload-alt",
        menu = Main.Setting, order = "7",
        name = {"版本", "en:Product Version"},
        info = {"管理轻舟的运行版本，将轻舟升级到一个新的版本。注：升级包会立即下发，但在实例下次重启时生效。",
                "en:Manage the running version of the light boat and upgrade the light boat to a new version. Note: The upgrade package is delivered immediately, but takes effect the next time the instance is restarted."})
public class Version extends ModelBase implements qingzhou.api.type.List, Add, Show {
    @ModelField(
            create = false,
            search = true,
            name = {"产品版本", "en:Product Version"},
            info = {"产品的版本号。", "en:Version number of the product."})
    public String version;

    @ModelField(
            create = false,
            list = true,
            name = {"构建日期", "en:Build Date"},
            info = {"此版本的构建日期。", "en:The build date of this release."})
    public String buildDate;

    @ModelField(
            create = false,
            list = true, search = true,
            color = {"true:Green", "false:Gray"},
            name = {"生效中", "en:Running"},
            info = {"此版本是否处于生效状态。", "en:Whether this version is in effect."})
    public String running;

    @ModelField(
            create = false,
            input_type = InputType.markdown,
            name = {"发布说明", "en:Release Notes"},
            info = {"此版本的说明信息，通常会包括该版本的新增功能、修复已知问题等内容。",
                    "en:The description of this release, which usually includes new features in the release, fixes for known issues, and so on."})
    public String releaseNotes;

    @ModelField(
            input_type = InputType.bool,
            name = {"使用上传", "en:Enable Upload"},
            info = {"升级包可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The upgrade package can be uploaded from the client or read from a location specified on the server side."})
    public Boolean upload = false;

    @ModelField(
            display = "upload=false",
            required = true,
            file = true,
            name = {"文件位置", "en:Path"},
            info = {"服务器上升级包的位置。注：须为 version*.zip 类型的文件。",
                    "en:The location of the upgrade package on the server. Note: It must be a file of type version*.zip."})
    public String path;

    @ModelField(
            display = "upload=true",
            input_type = InputType.file,
            required = true,
            name = {"上传文件", "en:File"},
            info = {"上传一个文件到服务器，文件须是 version*.zip 类型的文件，否则可能会导致升级失败。",
                    "en:Upload a file to the server that must be of type version*.zip or the upgrade may fail."})
    public String file;

    @Override
    public String idField() {
        return "version";
    }

    @Override
    public boolean showOrderNumber() {
        return false;
    }

    @Override
    public boolean contains(String id) {
        String[] ids = allIds();
        for (String s : ids) {
            if (s.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private String[] allIds() {
        Set<String> ids = new HashSet<>();
        File[] listFiles = Main.getLibBase().listFiles();
        for (File file : Objects.requireNonNull(listFiles)) {
            String name = file.getName();
            if (name.endsWith(".zip")) name = name.substring(0, name.length() - 4);
            if (name.startsWith(Main.QZ_VER_NAME)) {
                ids.add(name.substring(Main.QZ_VER_NAME.length()));
            }
        }
        String[] versions = ids.toArray(new String[0]);
        Arrays.sort(versions, this::isLaterVersion);
        return versions;
    }

    @Override
    public List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws IOException {
        return ModelUtil.listData(allIds(), this::showData, pageNum, pageSize, showFields);
    }

    @ModelAction(
            code = Add.ACTION_CREATE, icon = "plus-sign",
            head_action = true,
            name = {"升级", "en:Upgrade"},
            info = {"将轻舟升级到一个新的版本。", "en:Upgrade the light boat to a new version."})
    public void create(Request request) throws Exception {
        getAppContext().invokeSuperAction(request);
    }

    @ModelAction(
            code = Add.ACTION_ADD, icon = "plus-sign",
            name = {"升级", "en:Upgrade"},
            info = {"将轻舟升级到一个新的版本。", "en:Upgrade the light boat to a new version."})
    public void add(Request request) {
        Main.invokeAgentOnInstances(request, DeployerConstants.ACTION_INSTALL_VERSION, Instance.allInstanceIds(null));
    }

    @ModelAction(
            code = Delete.ACTION_DELETE, icon = "trash",
            show = "running=false",
            list_action = true, order = "9", action_type = ActionType.action_list, distribute = true,
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) {
        Main.invokeAgentOnInstances(request, DeployerConstants.ACTION_UNINSTALL_VERSION, Instance.allInstanceIds(null));
    }

    @Override
    public Map<String, String> showData(String id) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("version", id);
        data.put("running", String.valueOf(getAppContext().getPlatformVersion().equals(id)));
        List<String> releaseNotes = getReleaseNotes(id);
        if (releaseNotes != null && !releaseNotes.isEmpty()) {
            StringBuilder content = new StringBuilder();
            releaseNotes.forEach(s -> content.append(s).append(System.lineSeparator()));
            data.put("releaseNotes", content.toString());
            String time = releaseNotes.get(releaseNotes.size() - 1);
            String flag = "Build_Time: ";
            if (time.startsWith(flag)) {
                data.put("buildDate", time.substring(flag.length()));
            }
        }
        return data;
    }

    private List<String> getReleaseNotes(String id) throws IOException {
        File versionFile = new File(Main.getLibBase(), Main.QZ_VER_NAME + id);
        String releaseNoteFile = "version-notes.md";

        if (versionFile.isDirectory()) {
            File notesFile = new File(versionFile, releaseNoteFile);
            if (notesFile.exists()) {
                return FileUtil.readLines(notesFile);
            }
        }

        versionFile = new File(Main.getLibBase(), Main.QZ_VER_NAME + id + ".zip");
        if (versionFile.isFile()) {
            if (versionFile.getName().toLowerCase().endsWith(".zip")) {
                try (ZipFile zipFile = new ZipFile(versionFile)) {
                    ZipEntry entry = zipFile.getEntry(releaseNoteFile);
                    if (entry != null) {
                        return FileUtil.readLines(zipFile.getInputStream(entry), StandardCharsets.UTF_8);
                    }
                }
            }
        }

        return null;
    }

    // NOTE：同 qingzhou.launcher.VersionUtil.isLaterVersion
    private int isLaterVersion(String v1, String v2) {
        if (v1 == null || v2 == null) return 0;

        v1 = v1.trim();
        v2 = v2.trim();
        String[] arr1 = v1.split("\\.");
        String[] arr2 = v2.split("\\.");

        int len1 = arr1.length;
        int len2 = arr2.length;
        int lim = Math.min(len1, len2);

        int k = 0;
        while (k < lim) {
            String c1 = arr1[k];
            String c2 = arr2[k];
            if (!c1.equals(c2)) {
                try {
                    return Integer.parseInt(c2) - Integer.parseInt(c1);
                } catch (NumberFormatException e) {
                    return c2.compareTo(c1);
                }
            }
            k++;
        }
        return len2 - len1;
    }

    @Override
    public void addData(Map<String, String> data) {
        throw new IllegalStateException("覆盖了 add 方法，不会再进入这里");
    }
}
