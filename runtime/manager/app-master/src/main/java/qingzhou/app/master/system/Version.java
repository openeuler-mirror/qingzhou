package qingzhou.app.master.system;

import qingzhou.api.*;
import qingzhou.api.type.Createable;

// todo 升级所有的节点版本，支持各版本切换，除正在使用的版本外皆可删除，切换后尚未重启的版本页可以删除
@Model(name = "version", icon = "upload-alt",
        menuName = "System", menuOrder = 2,
        nameI18n = {"系统版本", "en:System Version"},
        infoI18n = {"展示系统的版本信息，可将系统升级到一个新的版本上。",
                "en:Displays the version information of the system and can upgrade the system to a new version."})
public class Version extends ModelBase implements Createable {

    @ModelField(
            shownOnList = true,
            nameI18n = {"版本号", "en:ID"},
            infoI18n = {"此 Qingzhou 的版本号。", "en:The version number of this Qingzhou."})
    @FieldValidation(cannotAdd = true, cannotUpdate = true)
    public String id;

    @ModelField(
            showToEdit = false,
            nameI18n = {"使用上传", "en:Enable Upload"},
            infoI18n = {"安装的版本可以从客户端上传，也可以从服务器端指定的位置读取。",
                    "en:The installed version can be uploaded from the client or read from a location specified on the server side."})
    @FieldValidation(required = true, cannotUpdate = true)
    @FieldView(type = FieldType.bool)
    public boolean fileFrom = false;

    @ModelField(
            showToEdit = false,
            nameI18n = {"版本位置", "en:Version File"},
            infoI18n = {"服务器上版本的位置，通常是版本的文件，注：须为 *.zip 类型的文件。",
                    "en:The location of the version on the server, usually the version file, Note: Must be a *.zip file."})
    @FieldValidation(required = true, lengthMax = 255, unsupportedCharacters = "#", effectiveWhen = "fileFrom=false", cannotUpdate = true)
    public String filename;

    @ModelField(
            showToEdit = false,
            nameI18n = {"上传版本", "en:Upload Version"},
            infoI18n = {"上传一个版本文件到服务器，文件须是 *.zip 类型的 Qingzhou 版本文件。",
                    "en:Upload an version file to the server, the file must be a *.zip type qingzhou version file."})
    @FieldValidation(required = true, unsupportedCharacters = "#", effectiveWhen = "fileFrom=true", cannotUpdate = true)
    @FieldView(type = FieldType.file)
    public String fromUpload;
}
