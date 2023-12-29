package qingzhou.console.master.service;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.FieldType;
import qingzhou.api.console.Model;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.model.AddModel;
import qingzhou.api.console.model.EditModel;
import qingzhou.api.console.option.Option;
import qingzhou.api.console.option.OptionManager;
import qingzhou.console.Validator;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.master.MasterModelBase;
import qingzhou.console.sec.Encryptor;
import qingzhou.console.sec.SecureKey;
import qingzhou.console.util.Constants;
import qingzhou.console.util.ExceptionUtil;
import qingzhou.console.util.FileUtil;
import qingzhou.console.util.IPUtil;
import qingzhou.console.util.SafeCheckerUtil;
import qingzhou.console.util.StringUtil;
import qingzhou.console.util.XmlUtil;
import qingzhou.ssh.SSHService;
import qingzhou.ssh.SshResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Model(name = Constants.MODEL_NAME_node, icon = "node",
        menuName = "Service", menuOrder = 3,
        nameI18n = {"节点", "en:Node"},
        infoI18n = {"节点是对物理或虚拟计算机环境的抽象，是运行实例的基础设施。",
                "en:A node is an abstraction of a physical or virtual computer environment and is the infrastructure that runs instances."})
public class Node extends MasterModelBase implements AddModel {
    public static final String PASSWORD = "PASSWORD";
    public static final String CERTIFICATES = "CERTIFICATES";
    public static final String NODE_TYPE_SSH = "SSH";
    public static final String NODE_TYPE_MANUAL = "MANUAL";
    public static final String NODE_TYPE_REGISTERED = Constants.NODE_TYPE_REGISTERED;
    public static final String PASSWORD_ECHO = "***********";
    public static final String nodeZip = "nodezip";

    static {
        ConsoleContext consoleContext = ConsoleWarHelper.getMasterAppConsoleContext();
        if (consoleContext != null) {
            consoleContext.addI18N("node.type." + NODE_TYPE_SSH, new String[]{"SSH", "en:SSH"});
            consoleContext.addI18N("node.type." + NODE_TYPE_MANUAL, new String[]{"手动", "en:MANUAL"});
            consoleContext.addI18N("node.type." + NODE_TYPE_REGISTERED, new String[]{"自动注册", "en:REGISTERED"});

            consoleContext.addI18N("node.upload.dir", new String[]{"上传失败", "en:Upload Failed"});
            consoleContext.addI18N("node.start.failed", new String[]{"节点启动失败,具体原因请查看节点日志", "en:The node failed to be started. For details, see node logs"});
            consoleContext.addI18N("node.status.start", new String[]{"节点已经启动", "en:The node is started"});
            consoleContext.addI18N("node.status.started", new String[]{"节点未停止", "en:Node not stopped"});
            consoleContext.addI18N("node.check", new String[]{"节点不是一个合法的QingZhou", "en:Node is not a legitimate QingZhou"});
            consoleContext.addI18N("node.check.qingzhoubase", new String[]{"QingZhouBase不支持为空", "en:QingZhouBase cannot be empty"});
            consoleContext.addI18N("node.check.installationpath", new String[]{"QingZhou安装路径填写错误,请检查", "en:QingZhou installation path is filled in incorrectly, please check"});
            consoleContext.addI18N("node.check.node.installationpath", new String[]{"节点必须填写安装路径", "en:The SSH node must fill in the installation path"});
            consoleContext.addI18N("node.master.node.port", new String[]{"该端口被master占用，请填写其他端口", "en:This port is occupied by the master, please fill in the other ports"});
            consoleContext.addI18N("node.status.stop", new String[]{"无法连接到目标节点，可能是节点尚未启动或者IP端口信息有误",
                    "en:If you cannot connect to the target node, the node may not be started or the IP port information is incorrect"});
            consoleContext.addI18N("node.ssh.port", new String[]{"IP或者SSH端口不可用", "en:The IP or SSH port is unavailable"});
            consoleContext.addI18N("node.ssh.validatorpass", new String[]{"SSH 验证失败，请正确填写用户名、密码或者密钥",
                    "en:SSH authentication fails, please fill in the user name, password or key correctly"});
            consoleContext.addI18N("node.ssh.validatorOther", new String[]{"SSH验证失败，具体原因请查看日志", "en:SSH authentication failed. For details, see logs"});
            consoleContext.addI18N("node.stop.failed", new String[]{"节点停止失败", "en:The node failed to stop"});
            consoleContext.addI18N("node.delete.failed", new String[]{"节点删除失败", "en:Description The node failed to be deleted"});
            consoleContext.addI18N("node.qingzhouhome.notexist", new String[]{"qingzhouhome 不存在", "en:qingzhouhome does not exist"});
            consoleContext.addI18N("node.not.ssh", new String[]{"节点不支持SSH，请手动操作", "en:The node does not support SSH, please do it manually"});
            consoleContext.addI18N("node.manual.restart", new String[]{"升级包上传完成，请重启手动节点", "en:After the upgrade package is uploaded, restart the manual node"});
            consoleContext.addI18N("node.validatorPort.port", new String[]{"节点的ip:%s或者管理端口:%s不可用，请检查", "en:The node IP address :%s or management port :%s is unavailable"});
            consoleContext.addI18N("node.validatorSsh.sshPort", new String[]{"节点的ip:%s的SSH端口:%s不可用，请检查", "en:The node IP address :%s SSH port :%s is unavailable"});
            consoleContext.addI18N("node.update.old.nonexistent", new String[]{"节点ip:%s上%s路径下没有qingzhou，请检查", "en:There is no qingzhou under path %s on node IP: %s, please check"});
            consoleContext.addI18N("node.update.old.update", new String[]{"节点ip:%s上%s路径下的qingzhou，更新失败，信息:%s",
                    "en:QingZhou under path %s on node IP: %s, update failed, information: %s"});
            consoleContext.addI18N("node.add.port.check", new String[]{"节点ip:%s上端口%s-%s都不可用，请重新设置端口",
                    "en:Port %s-%s on node IP:%s is unavailable. Please reset the port"});
            consoleContext.addI18N("node.update.port.check", new String[]{"端口%s重复，请重新设置端口", "en:Port %s duplicate, reset the port"});
            consoleContext.addI18N("node.check.ip", new String[]{"节点的IP不支持是localhost或者127.开头的IP", "en:The IP of the node cannot be localhost or IP that begins with 127."});
            consoleContext.addI18N("node.check.name", new String[]{"节点的名称不支持使用 default", "en:The use of default is not supported for node names"});
            consoleContext.addI18N("node.check.installationPath", new String[]{"安装路径不支持以\\或者/结尾，不支持包含特殊字符和空格", "en:The installation path cannot end with \\ or / and cannot contain special and spaces characters"});
            consoleContext.addI18N("node.check.installationpath.empty", new String[]{"安装路径必须是个空目录", "en:The installation path must be an empty directory"});
            consoleContext.addI18N("node.check.remoteips", new String[]{"没有获取到远程服务器ip", "en:The remote server IP was not obtained"});
            consoleContext.addI18N("node.check.certificates", new String[]{"密钥不支持为空", "en:The key cannot be empty"});
            consoleContext.addI18N("node.check.certificates.format", new String[]{"密钥格式不正确，请复制秘钥文件所有内容", "en:The key format is incorrect, please copy all the contents of the key file"});
            consoleContext.addI18N("node.check.installationpath.duplicate", new String[]{"qingzhou的安装路径与其他节点存在重复，请更换路径", "en:The installation path of qingzhou is duplicated with other nodes, please change the path"});
            consoleContext.addI18N("node.check.port", new String[]{"该端口对应的进程不是QingZhou，请检查端口", "en:The process corresponding to this port is not QingZhou, please check the port"});
            consoleContext.addI18N("node.add.port.exist", new String[]{"端口%s已经被其他节点占用，请重新设置端口", "en:Port %s is already occupied by other nodes, please reset the port"});
            consoleContext.addI18N("node.update.port.occupied", new String[]{"端口%s已经占用，请重新设置端口", "en:Port %s is already occupied, please reset the port"});
            consoleContext.addI18N("node.ssh.password", new String[]{"密码", "en:PASSWORD"});
            consoleContext.addI18N("node.ssh.certificate", new String[]{"证书", "en:CERTIFICATE"});
            consoleContext.addI18N("node.add.manual.remote.call", new String[]{"请先在目标节点的”全局配置“中打开”支持集中管理“，并为其设置集中管理的”加密公钥“，生成“传输密钥”",
                    "en:First turn on Support centralized management in the Global Configuration of the target node, set the Centrally managed Authentication Public Key for it, and generate the Transmission Public Key"});
            consoleContext.addI18N("node.add.manual.remote.call.key", new String[]{"请确认“传输密钥”是否正确",
                    "en:The node qingzhou .xml is a demoteKey error"});
            consoleContext.addI18N("node.add.manual.port", new String[]{"请检查节点的管理端口[%s]是否正确",
                    "en:Please check if the management port[%s] of the node is correct"});
            consoleContext.addI18N("node.uploadFile.error", new String[]{"更新节点[%s]文件[%s]失败",
                    "en:Failed to update node [%s] file [%s]"});
            consoleContext.addI18N("node.add.manual.updatelocalkey.fail", new String[]{"更新节点[%s]的加密密钥失败",
                    "en:Failed to update encryption key for node [%s]."});
            consoleContext.addI18N("node.number.limit", new String[]{"集中管理的节点数被限制为：%s", "en:The number of nodes in centralized management is limited to: %s"});
        }
    }

    @ModelField(
            required = true, unique = true, showToList = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"唯一标识。", "en:Unique identifier."})
    public String id;

    @ModelField(required = true, showToList = true,
            isIpOrHostname = true, effectiveOnEdit = false,
            nameI18n = {"IP", "en:IP"}, unique = true,
            infoI18n = {"连接节点的 IP 地址。", "en:The IP address of the connected node."})
    public String ip;

    @ModelField(showToList = true, type = FieldType.number,
            required = true,
            isPort = true,
            nameI18n = {"管理端口", "en:Management Port"},
            infoI18n = {"节点的管理端口。", "en:The management port of the node."})
    public int port = 9060;

    @ModelField(type = FieldType.radio,
            required = true,
            showToList = true,
            nameI18n = {"注册方式", "en:Registry Type"},
            infoI18n = {"标识本节点的注册方式。注：SSH 方式主要适用于 Linux 平台。",
                    "en:Identifies how this node is registered. Note: SSH is mainly available for Linux platforms."})
    public String nodeCreationType = NODE_TYPE_SSH;

    @ModelField(showToList = true, type = FieldType.bool,
            effectiveWhen = "nodeCreationType=" + NODE_TYPE_SSH,
            nameI18n = {"开机自启", "en:Auto Start"}, infoI18n = {"设置开机后是否自动启动该节点。", "en:Set whether to automatically start the node after booting."})
    public boolean autostart = false;

    @ModelField(
            required = true,
            type = FieldType.number,
            isPort = true,
            effectiveWhen = "nodeCreationType=" + NODE_TYPE_SSH,
            nameI18n = {"SSH 端口", "en:SSH Port"},
            infoI18n = {"连接节点的 IP 地址的ssh服务使用的端口。", "en:Port used by the SSH service to connect the IP address of the node."})
    public int sshPort = 22;

    @ModelField(
            required = true,
            effectiveWhen = "nodeCreationType=" + NODE_TYPE_SSH,
            nameI18n = {"SSH 用户名", "en:SSH User Name "},
            infoI18n = {"SSH 登录使用的用户名。", "en:User name for SSH login"})
    public String sshUserName;

    @ModelField(type = FieldType.radio,
            effectiveWhen = "nodeCreationType=" + NODE_TYPE_SSH,
            nameI18n = {"认证方式", "en:Authentication method"},
            infoI18n = {"密码或者证书。", "en:Password login or secret key login."})
    public String passwordType = PASSWORD;

    @ModelField(type = FieldType.password,
            effectiveWhen = "passwordType=" + PASSWORD + "&" + "nodeCreationType=" + NODE_TYPE_SSH,
            nameI18n = {"SSH 密码", "en:SSH Password"},
            infoI18n = {"SSH 登录使用的密码", "en:Password or certificate."})
    public String sshPassword;

    @ModelField(type = FieldType.password, required = true, maxLength = 2048,
            effectiveWhen = "passwordType=" + CERTIFICATES + "&" + "nodeCreationType=" + NODE_TYPE_SSH,
            nameI18n = {"密钥字符串", "en:Secret Key String"},
            infoI18n = {"证书密钥字符串", "en:Certificate key Character string."})
    public String certificates;

    @ModelField(type = FieldType.select, required = true, maxLength = 2048,
            effectiveWhen = "passwordType=" + CERTIFICATES + "&" + "nodeCreationType=" + NODE_TYPE_SSH,
            nameI18n = {"密钥对类型", "en:Key Pair Type"},
            infoI18n = {"证书密钥对类型", "en:Certificate Key Pair Type."})
    public String keyPairType = "";// TODO SshClient.SSH_RSA;

    @ModelField(
            required = true,
            effectiveOnEdit = false,
            effectiveWhen = "nodeCreationType=" + NODE_TYPE_SSH,
            nameI18n = {"安装路径", "en:Installation Path"},
            infoI18n = {"QingZhou 安装路径，即 ${qingzhou.home}。当路径不存在时，系统自动创建，不支持修改。当“注册方式”选择为 “SSH” 类型时，必填，且必须是空目录；手动节点可以自动获得。",
                    "en:QingZhou installation path, that is, ${qingzhou.home}, the path does not exist automatically created," +
                            " does not support modification, SSH type must be filled in, must be an empty directory, manual node can be obtained automatically."})
    public String installationPath;

    @ModelField(
            effectiveWhen = "nodeCreationType=" + NODE_TYPE_SSH,
            nameI18n = {"JAVA HOME", "en:JAVA HOME"},
            infoI18n = {"节点的 JAVA HOME。", "en:Java home of the node."})
    public String javaHome;

    @ModelField(showToList = true, effectiveOnCreate = false, effectiveOnEdit = false,
            type = FieldType.bool,
            nameI18n = {"运行中", "en:Running"}, infoI18n = {"了解该组件的运行状态。", "en:Know the operational status of the component."})
    public boolean running;

    @Override
    public OptionManager fieldOptions(Request request, String fieldName) {
        if ("nodeCreationType".equals(fieldName)) {
            List<Option> options = new ArrayList<>();
            options.add(Option.of(NODE_TYPE_SSH, new String[]{"SSH", "en:SSH"}));
            options.add(Option.of(NODE_TYPE_MANUAL, new String[]{"手动", "en:MANUAL"}));
            if (EditModel.ACTION_NAME_EDIT.equals(request.getActionName())) {
                try {
                    Map<String, String> node = getDataStore().getDataById(Constants.MODEL_NAME_node, request.getId());
                    if (node != null && !node.isEmpty()) {
                        if (NODE_TYPE_REGISTERED.equals(node.get("nodeCreationType"))) {
                            options.add(Option.of(NODE_TYPE_REGISTERED, new String[]{"自动注册", "en:REGISTERED"}));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            return () -> options;
        }

        if ("passwordType".equals(fieldName)) {
            List<Option> options = new ArrayList<>();
            options.add(Option.of(PASSWORD, new String[]{"密码", "en:PASSWORD"}));
            options.add(Option.of(CERTIFICATES, new String[]{"证书", "en:CERTIFICATE"}));
            return () -> options;
        }

        if ("keyPairType".equals(fieldName)) {
            return () -> new ArrayList<>(Arrays.asList(
                    Option.of("ssh-rsa"),
                    Option.of("ssh-dss"),
                    Option.of("ssh-ed25519"))// TODO TWSshClient
            );
        }

        return super.fieldOptions(request, fieldName);
    }

    @Override
    public String validate(Request request, String fieldName) {
        String newValue = request.getParameter(fieldName);
        if ("name".equals(fieldName)) {// for #ITAIT-5658
            if (StringUtil.notBlank(newValue)) {
                if ("default".equals(newValue)) {
                    return getConsoleContext().getI18N("node.check.name");
                }
            }
        }
        if ("ip".equals(fieldName)) {
            if (StringUtil.notBlank(newValue)) {
                if (("localhost").equals(newValue) || newValue.startsWith("127.")) {
                    return getConsoleContext().getI18N("node.check.ip");
                }
            }
        }
        if ("installationPath".equals(fieldName)) {
            if (StringUtil.notBlank(newValue)) {
                // 强校验 路径中不能存在
                if (checkInstallationPath(newValue)) {
                    return getConsoleContext().getI18N("node.check.installationPath");

                }
            }
        }

        if ("javaHome".equals(fieldName)) {
            if (StringUtil.notBlank(newValue)) {
                // 强校验 路径中不能存在
                String risk;
                if (StringUtil.notBlank(risk = SafeCheckerUtil.hasCommandInjectionRiskWithSkip(newValue, null))) {
                    return Validator.dataInvalidMsg(risk);
                }
            }
        }

        if ("certificates".equals(fieldName)) {
            String passwordType = request.getParameter("passwordType");
            if (StringUtil.notBlank(passwordType) && CERTIFICATES.equals(passwordType)) {
                // 校验certificates 格式
                if (StringUtil.isBlank(newValue)) {
                    return getConsoleContext().getI18N("node.check.certificates");
                } else {
                    if (!certificatesFormat(newValue)) {
                        return getConsoleContext().getI18N("node.check.certificates.format");
                    }
                }
            }
        }

        return super.validate(request, fieldName);
    }

    private boolean certificatesFormat(String newValue) {
        return newValue.startsWith("-----BEGIN") && newValue.indexOf("-----END") > 0 || PASSWORD_ECHO.equals(newValue);
    }

    private boolean checkInstallationPath(String newValue) {
        List<String> illegalCollections = Arrays.asList("|", "&", "~", "../", "./", ":", "*", "?", "\"", "'", "<", ">", "(", ")", "[", "]", "{", "}", "^", " ");
        for (String illegalCollection : illegalCollections) {
            if (newValue.contains(illegalCollection)) {
                return true;
            }
        }

        return newValue.endsWith("/") || newValue.endsWith("\\");
    }

    @Override
    public void add(Request request, Response response) throws Exception {
        Map<String, String> node = prepareParameters(request);
        String nodeType = node.get("nodeCreationType");
        if (NODE_TYPE_SSH.equals(nodeType)) {
            addSshNode(response, node);
        } else {
            addManualNode(response, node);
        }
    }

    private void addSshNode(Response response, Map<String, String> node) throws Exception {
        // 校验ssh client
        validatorSsh(response, node);
        if (!response.isSuccess()) {
            return;
        }

        File destNodeTempDir = FileUtil.newFile(ConsoleWarHelper.getHome(), nodeZip);
        SSHService sshClient = getSshClient(node);
        try {
            // 校验node是否重复
            List<Map<String, String>> nodeList = getDataStore().getAllData(Constants.MODEL_NAME_node);
            int maxNodeNumber = Integer.MAX_VALUE;// TODO LicUtils.limitNodes();
            if (nodeList != null && nodeList.size() >= maxNodeNumber) {
                response.setSuccess(false);
                response.setMsg(String.format(getConsoleContext().getI18N("node.number.limit"), maxNodeNumber));
                return;
            }
            checkInstallationPathDuplicated(response, node, nodeList, sshClient);
            if (!response.isSuccess()) {
                return;
            }

            // 校验端口是否重复
            autoAvailablePort(response, nodeList, node);
            if (!response.isSuccess()) {
                return;
            }

            // 校验Java环境
            javaCmd(sshClient, response, node);
            if (!response.isSuccess()) {
                return;
            }

            // 构建节点包
            buildNodeZip(response, destNodeTempDir, node);
            if (!response.isSuccess()) {
                return;
            }

            // 上传节点包
            final File nodeZipFile = FileUtil.newFile(destNodeTempDir.getCanonicalPath() + ".zip");
            try {
                FileUtil.zipFiles(destNodeTempDir, nodeZipFile, false);

                if (upload(nodeZipFile, sshClient, response, node)) {
                    //encryptParameters(node);
                    getDataStore().addData(Constants.MODEL_NAME_node, node.get(FIELD_NAME_ID), node);
                } else {
                    return;
                }
            } catch (IOException e) {
                return;
            } finally {
                FileUtil.forceDeleteQuietly(nodeZipFile);
            }
        } finally {
            sshClient.close();
            FileUtil.forceDeleteQuietly(destNodeTempDir);
        }
    }

    private void addManualNode(Response response, Map<String, String> node) throws Exception {
        List<Map<String, String>> nodeList = getDataStore().getAllData(Constants.MODEL_NAME_node);
        String ip = node.get("ip");
        String port = node.getOrDefault("port", "9060");
        boolean portRepeat = checkPortRepeat(nodeList, "", port, ip);
        if (portRepeat) {
            response.setSuccess(false);
            response.setMsg(String.format(getConsoleContext().getI18N("node.add.port.exist"), port));
            return;
        }

        List<String> localIps = IPUtil.getLocalIpsAsFull();
        if (localIps != null && localIps.size() > 0) {
            if (localIps.contains(ip)) {
                // 判断端口是否是master在用的端口
                Set<String> ports = getUsedPorts(Paths.get(ConsoleWarHelper.getHome().getCanonicalPath(), "domains"));
                if (ports.size() > 0) {
                    if (ports.contains(port)) {
                        response.setSuccess(false);
                        response.setMsg(getConsoleContext().getI18N("node.master.node.port"));
                        return;
                    }
                }
            }
        }

        if (!IPUtil.pingIpPort(ip, Integer.parseInt(port))) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("node.status.stop"));
            return;
        }
        /*Response remoteQingZhouBase = null;
        String remoteKey = null;
        try {
            remoteKey = Encryptor.encrypt(Encryptor.decryptWithPrivateKey(node.get(SecureKey.remoteKeyName)));
            node.put(SecureKey.remoteKeyName, remoteKey);

            remoteQingZhouBase = RemoteClient.sendReq(String.format("http://%s:%s%s%s", ip, port, Constants.remoteApp, Constants.remotePath), null, remoteKey);
            if (remoteQingZhouBase != null) {
                response.setSuccess(false);
                response.setMsg(getConsoleContext().getI18N("node.check.qingzhoubase"));
                return;
            }
        } catch (FileNotFoundException e) {
            if (e.getMessage() != null && e.getMessage().contains("remote/callServer")) {
                response.setSuccess(false);
                response.setMsg(getConsoleContext().getI18N("node.add.manual.remote.call"));
                return;
            }
        } catch (RuntimeException runtimeException) {
            final String message = runtimeException.getMessage();
            if (message.contains("Security key format error") || message.contains("remoteKey error")) {
                response.setSuccess(false);
                response.setMsg(getConsoleContext().getI18N("node.add.manual.remote.call.key"));
                return;
            } else {
                throw runtimeException;
            }
        }

        if (remoteQingZhouBase == null) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("node.check.qingzhoubase"));
            return;
        }

        if (!remoteQingZhouBase.endsWith(Constants.DEFAULT_DOMAIN)) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("node.check"));
            return;
        }

        int lastIndexOf = remoteQingZhouBase.lastIndexOf(Constants.DOMAINS_DIR_NAME);
        if (lastIndexOf < 1) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("node.check.qingzhoubase"));
            return;
        }

        boolean updateLocalKey;
        try {
            PasswordCipher cipher = new PasswordCipherImpl(Encryptor.decrypt(remoteKey));
            String localKey = cipher.encrypt(Encryptor.getOrInitKey(ConsoleWarHelper.getDomain(), SecureKey.localKeyName));
            updateLocalKey = RemoteClient.sendReq(String.format("http://%s:%s%s%s", ip, port, Constants.remoteApp, Constants.remotePath), new Object[]{localKey}, "updateLocalKey");
        } catch (Exception e) {
            updateLocalKey = false;
        }
        if (!updateLocalKey) {
            response.setSuccess(false);
            response.setMsg(String.format(getConsoleContext().getI18N("node.add.manual.updatelocalkey.fail"), actionContext.getId()));
            return;
        }

        String installationPath = remoteQingZhouBase.substring(0, lastIndexOf - 1);
        node.put("installationPath", installationPath);*/
        node.put("autostart", "false");
        getDataStore().addData(Constants.MODEL_NAME_node, node.get(FIELD_NAME_ID), node);
    }

    @Override
    public void update(Request request, Response response) throws Exception {

    }

    private static Set<String> getUsedPorts(Path domainsPath) {
        Set<String> ports = new HashSet<>();
        try (Stream<Path> stream = Files.list(domainsPath)) {
            List<Path> paths = stream.collect(Collectors.toList());
            for (Path path : paths) {
                File serverXml = Paths.get(path.toString(), "conf", "server.xml").toFile();
                if (!serverXml.exists()) {
                    continue;
                }
                XmlUtil xmlUtils = new XmlUtil(serverXml);
                List<String> tempPort = xmlUtils.getAttributeList("//@port");
                ports.addAll(tempPort);
            }

        } catch (Exception ignore) {
        }

        return ports;
    }

    private boolean upload(File dest, SSHService sshClient, Response response, Map<String, String> node) throws Exception {
        String installationPath = node.get("installationPath");
        if (!installationPath.endsWith("/")) {
            installationPath += "/";
        }
        // 1. 创建要安装的目录 无论存在与否都进行创建
        sshClient.execCmd(buildCommand(SshCommand.MKDIR, installationPath));
        try {
            String destName = dest.getName();
            String destZip = installationPath + destName;
            String res = sshClient.uploadFile(dest.getCanonicalPath(), destZip);
            if ("ok".equals(res)) {
                sshClient.execCmd(buildCmd(buildCommand(SshCommand.CD, installationPath),
                        "&&", buildCommand(SshCommand.UNZIP, destName),
                        "&&", buildCommand(SshCommand.UNINSTALL, destName)));
                try {
                    sshClient.execCmd(buildCmd(buildCommand(SshCommand.CHMOD, installationPath)));// for #NC-3078、#NC-3290
                } catch (Exception ignored) {
                    // 忽略异常，因为windows版本可能没有sh脚本，导致创建的节点没有sh脚本，从而执行授权命令失败。异常之后后续无法启动。
                }
            } else {
                response.setSuccess(false);
                response.setMsg(getConsoleContext().getI18N("node.upload.dir"));
            }
        } finally {
            if (!response.isSuccess()) {
                //  如果上传失败自动删除
                sshClient.execCmd(buildCommand(SshCommand.UNINSTALL, installationPath));
            }
        }

        return response.isSuccess();
    }

    private void buildNodeZip(Response response, File destNodeTempDir, Map<String, String> node) throws Exception {
        File qzHome = ConsoleWarHelper.getHome();
        if (!qzHome.exists()) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("node.qingzhouhome.notexist"));
            return;
        }

        if (destNodeTempDir.exists()) {
            FileUtil.forceDeleteQuietly(destNodeTempDir);
        }

        File libDir = ConsoleWarHelper.getLibDir();
        FileUtil.copyFileOrDirectory(libDir, FileUtil.newFile(destNodeTempDir, "framework", libDir.getName()));
        copyBin(FileUtil.newFile(qzHome, "bin"), FileUtil.newFile(destNodeTempDir, "bin"), true, false);
        FileUtil.copyFileOrDirectory(FileUtil.newFile(qzHome, "apps"), FileUtil.newFile(destNodeTempDir, "apps"));
        copyLicFile(destNodeTempDir);
        buildNewDomain1(destNodeTempDir, node);
    }

    private void buildNewDomain1(File destNodeHome, Map<String, String> node) throws Exception {
        File destDomain1Dir = FileUtil.newFile(destNodeHome, "domains", "domain1");
        if (!destDomain1Dir.exists()) {
            FileUtil.mkdirs(destDomain1Dir);
        }
        String remoteKey = SecureKey.getSecureKey(destDomain1Dir, SecureKey.remoteKeyName, args -> Encryptor.getOrInitKey(destDomain1Dir, SecureKey.remoteKeyName));
        SecureKey.writeSecureKey(destDomain1Dir, SecureKey.localKeyName, Encryptor.getOrInitKey(ConsoleWarHelper.getDomain(), SecureKey.localKeyName)); // for #NC-3321

        node.put(SecureKey.remoteKeyName, Encryptor.encrypt(remoteKey));
        File domainTemplateDir = FileUtil.newFile(destNodeHome.getParent(), "framework", ConsoleWarHelper.getLibDir().getName(), "domain_template");
        FileUtil.copyFileOrDirectory(domainTemplateDir, destDomain1Dir);
        FileUtil.forceDeleteQuietly(FileUtil.newFile(destDomain1Dir, "conf", "console.xml"));

        // 更新端口
        XmlUtil xmlUtil = new XmlUtil(FileUtil.newFile(destDomain1Dir, "conf", "server.xml"));
        String port = node.get("port");
        if (!"9060".equals(port)) {
            xmlUtil.setAttribute("//server/connector", "port", port);
        }

        String javaHome = node.get("javaHome");
        if (StringUtil.notBlank(javaHome)) {
            File envFile = FileUtil.newFile(destNodeHome.getParent(), "bin", "JAVA_HOME.txt");
            FileUtil.writeFile(envFile, javaHome);
        }
        xmlUtil.setAttribute("//start-args/arg[@name='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:8888']", "enabled", "false");
        xmlUtil.setAttribute("//root/server", "remoteSupported", "true");
        String globalIp = node.get("ip");
        if (StringUtil.notBlank(globalIp)) {
            xmlUtil.setAttribute("//root/server", "globalIp", globalIp);
        }
        xmlUtil.write();
    }


    public static void copyLicFile(File destDir) {
        try {
            File licenseFile;
            String licenseDir = new XmlUtil(ConsoleWarHelper.getServerXml()).getSpecifiedAttribute("/root/server", "licenseDir");
            if (StringUtil.isBlank(licenseDir)) {
                licenseFile = FileUtil.newFile(ConsoleWarHelper.getHome(), "license.dat");
            } else {
                licenseFile = FileUtil.newFile(licenseDir, "license.dat");
            }
            if (licenseFile.exists()) {
                FileUtil.copyFileOrDirectory(licenseFile, FileUtil.newFile(destDir, "license.dat"));
            }
        } catch (Exception ignored) {
        }
    }

    public static void copyBin(File src, File dest, boolean commandsTool, boolean copyWin) throws IOException {
        FileUtil.copyFileOrDirectory(FileUtil.newFile(src, "qingzhou-launcher.jar"), FileUtil.newFile(dest, "qingzhou-launcher.jar"));
        List<String> binNames = new ArrayList<>(Arrays.asList("admin", "forcestop", "startd", "startserver", "stopserver", "version"));
        if (commandsTool) {
            binNames.add("commandstool");
            binNames.add("cli");
        }
        for (String binName : binNames) {
            FileUtil.copyFileOrDirectory(FileUtil.newFile(src, binName + ".sh"), FileUtil.newFile(dest, binName + ".sh"));
            if (copyWin) {
                FileUtil.copyFileOrDirectory(FileUtil.newFile(src, "windows", binName + ".bat"), FileUtil.newFile(dest, "windows", binName + ".bat"));
            }
        }
    }

    private void javaCmd(SSHService sshClient, Response response, Map<String, String> node) throws Exception {
        SshResult sshResult;
        String[] cmd;
        try {
            String javaHome = node.get("javaHome");
            String javaVersion = buildCommand(SshCommand.JAVA_VERSION, null);
            if (!StringUtil.isBlank(javaHome)) {
                cmd = new String[]{buildCommand(SshCommand.EXPORT_JAVA_HOME, javaHome), "&&", javaVersion};
            } else {
                cmd = new String[]{javaVersion};
            }
            sshResult = sshClient.execShell(buildCmd(cmd));
            // TODO 兼容 openjdk 毕昇jdk jdk
            // sshResult.handleCmdExecResult("Runtime Environment");
            if (!sshResult.isSucceed()) {
                response.setSuccess(false);
                response.setMsg(sshResult.getMessage().replace("\n", ""));
            }
        } catch (IOException e) {
            response.setSuccess(false);
            response.setMsg(e.getMessage());
        }
    }


    private void autoAvailablePort(Response response, List<Map<String, String>> nodeList, Map<String, String> node) {
        String port = node.getOrDefault("port", "9060");
        String ip = node.get("ip");
        boolean flag = false;
        int port0 = Integer.parseInt(port);
        int i = 0;
        while (i < 10) {
            if (!IPUtil.pingIpPort(ip, port0)) {
                // 判断是否重复
                boolean portRepeat = checkPortRepeat(nodeList, "", String.valueOf(port0), ip);
                if (!portRepeat) {
                    flag = true;
                    break;
                }
                if (port0 == 65535) {
                    break;
                }
            }
            i++;
            port0++;
        }
        if (!flag) {
            response.setSuccess(false);
            response.setMsg(String.format(getConsoleContext().getI18N("node.add.port.check"), ip, port, port0));
        } else {
            node.put("port", String.valueOf(port0));
        }
    }

    private boolean checkPortRepeat(List<Map<String, String>> nodeList, String nodeName, String port, String ip) {
        if (nodeList == null || nodeList.size() < 1) {
            return false;
        }

        for (Map<String, String> node : nodeList) {
            if (StringUtil.notBlank(nodeName)) {
                if (nodeName.equals(node.get("name"))) {
                    continue;
                }
            }
            String port0 = node.get("port");
            String ip0 = node.get("ip");
            String ipStr = ip.replace("localhost", "").replace("127.0.0.1", "");
            String ip0Str = ip0.replace("localhost", "").replace("127.0.0.1", "");

            if ((port0 + ip0Str).equals(port + ipStr)) {
                return true;
            }
            if ((port0 + ip0).equals(port + ip)) {
                return true;
            }
        }

        return false;
    }

    private void checkInstallationPathDuplicated(Response response, Map<String, String> node, List<Map<String, String>> nodeList, SSHService sshClient) throws Exception {
        // 获取远程服务器所有 ip地址
        SshResult ipResult = sshClient.execShell(buildCommand(SshCommand.IFCONFIG, null));
        List<String> ips = null;
        String message = ipResult.getMessage();
        if (StringUtil.notBlank(message)) {
            ips = Arrays.stream(message.split(",")).filter(StringUtil::notBlank).collect(Collectors.toList());
        }
        if (ips == null || ips.size() < 1) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("node.check.remoteips"));
            return;
        }
        String installationPath = node.get("installationPath");

        SshResult installationPathResult = sshClient.execShell(buildCommand(SshCommand.LS, installationPath));
        int num;
        try {
            num = Integer.parseInt(installationPathResult.getMessage().replaceAll("[\n]", ""));
        } catch (NumberFormatException e) {
            num = 0;
        }
        if (num != 0) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("node.check.installationpath.empty"));
            return;
        }

        boolean checkInstallationPath = false;
        if (nodeList != null && nodeList.size() > 0) {
            for (Map<String, String> nodeProperty : nodeList) {
                String ipOld = nodeProperty.get("ip");
                String installationPathOld = nodeProperty.get("installationPath");
                if (ips.contains(ipOld)) {
                    // 安装路径不支持相等  不支持相互包含
                    if (installationPath.equals(installationPathOld)) {
                        checkInstallationPath = true;
                        break;
                    }
                }
            }
        }

        if (checkInstallationPath) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("node.check.installationpath.duplicate"));
        }
    }

    private SSHService getSshClient(Map<String, String> node) throws Exception {
        try {
            if (!PASSWORD.equals(node.get("passwordType"))) {
                String certificates = node.get("certificates");
                if (StringUtil.notBlank(certificates)) {
                    if (certificatesFormat(certificates)) {
                        // 生成密钥文件
                        File tempFile = geTempFile(node.get(FIELD_NAME_ID));
                        if (tempFile.exists()) {
                            tempFile.delete();
                        }
                        tempFile.createNewFile();
                        node.put("privateKeyLocation", tempFile.getCanonicalPath());
                        String invertResult = invertResult(certificates);
                        try (FileWriter fileWriter = new FileWriter(tempFile)) {
                            fileWriter.write(invertResult);
                        }
                    } else {
                        throw new RuntimeException(certificates + " is malformed, copy the full key file contents");
                    }
                }
            }

            return ConsoleWarHelper.getAppContext(Constants.QINGZHOU_MASTER_APP_NAME).getService(SSHService.class).start(node);
        } catch (Exception e) {
            String privateKeyLocation = node.get("privateKeyLocation");
            if (StringUtil.notBlank(privateKeyLocation)) {
                File file = new File(privateKeyLocation);
                if (file.exists()) {
                    FileUtil.forceDelete(file);
                }
            }
            throw e;
        }
    }

    private File geTempFile(String name) {
        String fileName = "ssh_" + name;
        String basePath = "conf" + File.separator + fileName;
        return FileUtil.newFile(ConsoleWarHelper.getDomain(), basePath);
    }

    private String invertResult(String val) {
        //将空格替换为换行符
        int start = val.indexOf("-- ");
        int end = val.lastIndexOf("--END");
        StringBuilder stringBuilder = new StringBuilder();
        String prefix = val.substring(0, start);
        stringBuilder.append(prefix);
        String replace = val.substring(start, end).replace(" ", System.getProperty("line.separator"));
        stringBuilder.append(replace);
        stringBuilder.append(val.substring(end));
        return stringBuilder.toString();
    }

    private void validatorSsh(Response response, Map<String, String> node) {
        final String ip = node.get("ip");
        final int sshPort = Integer.parseInt(node.getOrDefault("sshPort", "22"));
        boolean portOpen = IPUtil.pingIpPort(ip, sshPort);
        if (!portOpen) {
            response.setSuccess(false);
            response.setMsg(String.format(getConsoleContext().getI18N("node.validatorSsh.sshPort"), ip, sshPort));
        }
    }

    private static String buildCmd(String... strings) {
        return String.join(" ", strings).trim();
    }

    public static String buildCommand(SshCommand command, String arg) {
        String cmd = command.getCommand();
        if (StringUtil.isBlank(arg)) {
            return cmd;
        } else {
            String risk;
            if (StringUtil.notBlank(risk = SafeCheckerUtil.hasCommandInjectionRiskWithSkip(arg, null))) { // fix #ITAIT-4940 , #NC-1705
                throw new IllegalArgumentException("This command may have security risks (" + risk + "): " + arg);
            }

            if (command == SshCommand.UNINSTALL || command == SshCommand.MKDIR || command == SshCommand.UNZIP) {
                FileUtil.newFile(arg);// 校验目录
                if (arg.equals("/") || arg.equals("/*") || arg.contains("*")) {
                    throw ExceptionUtil.unexpectedException();
                }
            }

            return String.format(cmd, arg);
        }
    }

    enum SshCommand {
        EXPORT_JAVA_HOME("export JAVA_HOME=%s && export PATH=$JAVA_HOME/bin:$PATH"),
        JAVA_VERSION("java -version"),
        MKDIR("mkdir -p %s"),
        UNINSTALL("rm -rf %s"),
        CHMOD("chmod +x %sbin/*.sh"),
        START("java -jar %sbin/qingzhou-launcher.jar server startd"),
        STOP("java -jar %sbin/qingzhou-launcher.jar server stop"),
        FORCE_STOP("java -jar %sbin/qingzhou-launcher.jar server forcestop"),
        IFCONFIG("ifconfig | grep 'inet' | grep -v 127 |grep -v ::1 |awk '{printf(\"%s,\",$2)}'"),
        LS("ls %s | wc -l"),
        CD("cd %s"),
        UNZIP("unzip %s");

        final String command;

        SshCommand(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }
    }
}
