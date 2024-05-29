package qingzhou.app.master.system;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Listable;
import qingzhou.app.master.MasterApp;
import qingzhou.engine.util.Utils;
import qingzhou.logger.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Model(code = "version", icon = "upload-alt",
        menu = "System", order = 2,
        name = {"系统版本", "en:System Version"},
        info = {"展示系统的版本信息，可将系统升级到一个新的版本上。",
                "en:Displays the version information of the system and can upgrade the system to a new version."})
public class Version extends ModelBase implements Createable {

    @ModelField(
            list = true,
            name = {"版本号", "en:ID"},
            info = {"此 Qingzhou 的版本号。", "en:The version number of this Qingzhou."})
    public String id;

    @ModelField(
            name = {"使用上传", "en:Enable Upload"},
            info = {"安装的版本可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The installed version can be uploaded from the client or read from a location specified on the server side."})
    public boolean fileFrom = false;

    @ModelField(
            show = "fileFrom=false",
            name = {"版本位置", "en:Version File"},
            info = {"服务器上版本的位置，通常是版本的文件，注：须为 *.zip 类型的文件。",
                    "en:The location of the version on the server, usually the version file, Note: Must be a *.zip file."})
    public String filename;

    @ModelField(
            show = "fileFrom=true",
            name = {"上传版本", "en:Upload Version"},
            info = {"上传一个版本文件到服务器，文件须是 *.zip 类型的 Qingzhou 版本文件。",
                    "en:Upload an version file to the server, the file must be a *.zip type qingzhou version file."})
    public String fromUpload;

    @ModelField(list = true, createable = false, editable = false,
            type = FieldType.bool,
            name = {"生效中", "en:effecting"}, info = {"是否是当前正在运行的版本。", "en:Is it the currently running version."})
    public boolean running;

    @ModelAction(disable = true,
            name = {"创建", "en:Create"},
            info = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    public void create(Request request, Response response) throws Exception {
    }

    @ModelAction(disable = true,
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request, Response response) throws Exception {
    }

    @ModelAction(disable = true,
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
    }

    @ModelAction(disable = true,
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
    }

    @ModelAction(disable = true,
            name = {"删除", "en:Delete"},
            info = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
    }

    @ModelAction(
            name = {"列表", "en:List"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void list(Request request, Response response) throws Exception {
        List<Map<String, String>> list = new ArrayList<>();
        Set<String> versionList = new HashSet<>();
        for (File file : Objects.requireNonNull(MasterApp.getLibDir().getParentFile().listFiles())) {
            String version = retrieveVersion(file);
            if (version != null && !version.isEmpty()) {
                versionList.add(version);
            }
        }
        for (String v : versionList) {
            Map<String, String> p = new HashMap<>();
            p.put(Listable.FIELD_NAME_ID, v);
            p.put("running", String.valueOf(MasterApp.getLibDir().getName().equals(versionFlag + v)));
            list.add(p);
        }
        list.sort((o1, o2) -> isLaterVersion(o2.get(Listable.FIELD_NAME_ID), o1.get(Listable.FIELD_NAME_ID)) ? 1 : -1);

        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getParameter(Listable.PARAMETER_PAGE_NUM));
        } catch (NumberFormatException ignored) {
        }

        int pageSize = response.getPageSize();
        if (pageSize == -1) {
            pageSize = 10;
        }

        int totalSize = list.size();
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);

        List<Map<String, String>> pagedVersions = list.subList(startIndex, endIndex);
        for (Map<String, String> version : pagedVersions) {
            response.addData(version);
        }

        response.setTotalSize(totalSize);
        response.setPageSize(pageSize);
        response.setPageNum(pageNum);
    }

    private String versionFlag = "version";

    private String retrieveVersion(File file) {
        String fileName = file.getName();
        if (!fileName.startsWith(versionFlag)) {
            return null;
        }

        if (file.isFile()) {
            String format = ".zip";
            boolean zip = fileName.toLowerCase().endsWith(format);
            if (zip) {
                try {
                    try (ZipFile zipFile = new ZipFile(file)) {
                        ZipEntry entry = zipFile.getEntry("qingzhou-api.jar");
                        if (entry != null) {
                            return fileName.substring(versionFlag.length(), fileName.length() - format.length());
                        }
                    }
                } catch (Exception e) {
                    MasterApp.getService(Logger.class).warn("Failed to parse fileName: " + fileName);
                }
            }
        } else if (file.isDirectory()) {
            if (Utils.newFile(file, "engine", "qingzhou-engine.jar").exists()) {
                return fileName.substring(versionFlag.length());// 解压后的版本文件
            }
        }
        return null;
    }

    private static boolean isLaterVersion(String v1, String v2) {
        if (v1 == null || v2 == null) return false;

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
                    return Integer.parseInt(c1) > Integer.parseInt(c2);
                } catch (NumberFormatException e) {
                    return c1.compareTo(c2) > 0;
                }
            }
            k++;
        }
        return len1 > len2;
    }
}
