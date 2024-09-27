package qingzhou.app.system;

import qingzhou.api.*;
import qingzhou.api.type.Add;
import qingzhou.api.type.Delete;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;


@Model(code = DeployerConstants.MODEL_UPGRADE_AGENT,
        hidden = true,
        name = {"产品升级代理", "en:product upgrade Agent"},
        info = {"", "en:"})
public class UpgradeAgent extends ModelBase {

    @ModelField(
            show = "upload=true",
            type = FieldType.file,
            name = {"", "en:"},
            info = {"", "en:"})
    public String file;

    @Override
    public void start() {
        getAppContext().addI18n("File.Not.Found", new String[]{"文件不存在", "en:file does not exist"});
        getAppContext().addI18n("File.Type.Error", new String[]{"需要zip格式的文件", "en:A zip format file is required"});
        getAppContext().addI18n("File.Name.Error", new String[]{"文件名不符合规则,需以version开头", "en:The file name does not comply with the rules and needs to start with version"});
        getAppContext().addI18n("File.Name.Exist", new String[]{"此版本已存在", "en:This version already exists"});
    }

    @ModelAction(
            code = Add.ACTION_ADD,
            name = {"", "en:"},
            info = {"", "en:"})
    public void addVersion(Request request) throws Exception {
        String filePath;
        String upload = request.getParameter("upload");
        if ("true".equals(upload)) {
            filePath = request.getParameter("file");
        } else {
            filePath = request.getParameter("path");
        }
        File newFile = new File(filePath);
        if (!newFile.exists()) {
            throw new Exception(getAppContext().getI18n("File.Not.Found"));
        }
        String fileName = newFile.getName();
        if (!fileName.toLowerCase().endsWith(".zip")) {
            throw new Exception(getAppContext().getI18n("File.Type.Error"));
        }

        String version = VersionUtil.getVer(newFile.getName());
        List<String> versions = VersionUtil.versionList().stream().map(map -> map.get("version")).collect(Collectors.toList());
        if (versions.contains(version)) {
            throw new Exception(getAppContext().getI18n("File.Name.Exist"));
        }
        if (Utils.notBlank(version)) {
            FileUtil.copyFileOrDirectory(newFile, new File(VersionUtil.getHomeDir().getCanonicalPath() + File.separator + "lib" + File.separator + newFile.getName()));
        } else {
            throw new Exception(getAppContext().getI18n("File.Name.Error"));
        }
    }

    @ModelAction(
            code = Delete.ACTION_DELETE,
            name = {"", "en:"},
            info = {"", "en:"})
    public void deleteVersion(Request request) throws Exception {
        String filePath = VersionUtil.getHomeDir().getCanonicalPath() + File.separator + "lib" + File.separator + VersionUtil.qzVerName + request.getId();
        File dir = new File(filePath);
        FileUtil.forceDelete(dir);
        File zip = new File(filePath + VersionUtil.format);
        FileUtil.forceDelete(zip);
    }
}
