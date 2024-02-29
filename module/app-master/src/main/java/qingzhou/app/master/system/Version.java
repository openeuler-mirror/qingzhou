package qingzhou.app.master.system;

import qingzhou.api.*;
import qingzhou.framework.util.FileUtil;

import java.io.File;

// todo 升级所有的节点版本，支持各版本切换，除正在使用的版本外皆可删除，切换后尚未重启的版本页可以删除
@Model(name = "version", icon = "upload-alt",
        menuName = "System", menuOrder = 1,
        nameI18n = {"系统版本", "en:System Version"},
        infoI18n = {"展示系统的版本信息，可将系统升级到一个新的版本上。",
                "en:Displays the version information of the system and can upgrade the system to a new version."})
public class Version extends ModelBase implements AddModel {

    @ModelField(
            showToList = true,
            disableOnCreate = true,
            disableOnEdit = true,
            nameI18n = {"版本号", "en:ID"},
            infoI18n = {"此轻舟的版本号。", "en:The version number of this Qingzhou."})
    public String id;

    @ModelField(
            required = true,
            disableOnEdit = true,
            showToEdit = false,
            type = FieldType.bool,
            nameI18n = {"使用上传", "en:Enable Upload"},
            infoI18n = {"安装的版本可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The installed version can be uploaded from the client or read from a location specified on the server side."})
    public boolean fileFrom = false;

    @ModelField(
            effectiveWhen = "fileFrom=false",
            disableOnEdit = true,
            showToEdit = false,
            required = true,
            notSupportedCharacters = "#",
            maxLength = 255,// for #NC-1418 及其它文件目录操作的，文件长度不能大于 255
            nameI18n = {"版本位置", "en:Version File"},
            infoI18n = {"服务器上版本的位置，通常是版本的文件，注：须为 *.zip 类型的文件。",
                    "en:The location of the version on the server, usually the version file, Note: Must be a *.zip file."})
    public String filename;

    @ModelField(
            type = FieldType.file,
            effectiveWhen = "fileFrom=true",
            disableOnEdit = true,
            showToEdit = false,
            notSupportedCharacters = "#",
            required = true,
            nameI18n = {"上传版本", "en:Upload Version"},
            infoI18n = {"上传一个版本文件到服务器，文件须是 *.zip 类型的轻舟版本文件。",
                    "en:Upload an version file to the server, the file must be a *.zip type qingzhou version file."})
    public String fromUpload;

    @Override
    public String resolveId(Request request) {
        File file;
        if (Boolean.parseBoolean(request.getParameter("fileFrom"))) {
            file = FileUtil.newFile(request.getParameter("fromUpload"));
        } else {
            file = new File(request.getParameter("filename"));
        }

        String fileName = file.getName();
        String suffix = ".zip";
        if (fileName.endsWith(suffix)) {
            return fileName.substring(0, fileName.length() - suffix.length());
        } else {
            return fileName;
        }
    }
}
