package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Listable;
import qingzhou.app.master.MasterApp;
import qingzhou.config.Config;
import qingzhou.engine.util.Utils;
import qingzhou.ssh.SSHConfig;
import qingzhou.ssh.SSHService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model(code = "node", icon = "node",
        menu = "Service", order = 4,
        name = {"节点", "en:Node"},
        info = {"节点是对物理环境的抽象，是 TongWeb 集中管理得以实施的基础设施，一台物理设备（准确的说是一个IP）只需要安装一个节点即可，在节点之上可以构建集群、实例、会话服务器、负载均衡器等逻辑组件。" +
                "在逻辑上，节点只做管理使用，不会参与用户应用的部署和运行（应用由 TongWeb 实例管理）。",
                "en:A node is an abstraction of the physical environment and is the infrastructure for the implementation of TongWeb centralized management. " +
                        "A physical device (to be precise, an IP) only needs to install a node, and a cluster, instance, and session server can be built on the node. , " +
                        "load balancer and other logical components. Logically, the node is only used for management, and will not participate in the deployment and " +
                        "operation of the user application (the application is managed by the TongWeb instance)."})
public class Node extends ModelBase implements Createable {
    @ModelField(
            list = true,
            name = {"名称", "en:Name"},
            info = {"唯一标识。", "en:Unique identifier."})
    public String id;

    @ModelField(list = true,
            name = {"SSH 主机", "en:SSH Address"},
            info = {"连接节点的 IP 地址。", "en:The IP address of the connected node."})
    public String host;

    @ModelField(list = true,
            type = FieldType.number,
            port = true,
            name = {"SSH 端口", "en:SSH Port"},
            info = {"连接节点的 IP 地址的ssh服务使用的端口。", "en:Port used by the SSH service to connect the IP address of the node."})
    public int sshPort = 22;

    @ModelField(
            name = {"SSH 用户名", "en:SSH User Name "},
            info = {"SSH 登录使用的用户名。", "en:User name for SSH login"})
    public String sshUserName;

    @ModelField(
            type = FieldType.password,
            name = {"SSH 密码", "en:SSH Password"},
            info = {"SSH 登录使用的密码", "en:Password or certificate."})
    public String sshPassword;

    @ModelAction(
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request, Response response) throws Exception {
        if (getDataStore().exists(request.getParameter(Listable.FIELD_NAME_ID))) {
            response.setSuccess(false);
            response.setMsg(appContext.getI18n(request.getLang(), "validator.exist"));
            return;
        }
        Map<String, String> newNode = request.getParameters();
        getDataStore().addData(newNode.get(Listable.FIELD_NAME_ID), newNode);
    }

    @ModelAction(
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        show(request, response);
    }

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        DataStore dataStore = getDataStore();
        Map<String, String> data = dataStore.getDataById(request.getId());
        response.addData(data);
    }

    @ModelAction(
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        DataStore dataStore = getDataStore();
        Map<String, String> newData = request.getParameters();
        dataStore.updateDataById(request.getId(), newData);
    }

    @ModelAction(
            name = {"删除", "en:Delete"},
            info = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
        String id = request.getId();
        DataStore dataStore = getDataStore();
        dataStore.deleteDataById(id);
    }

    @ModelAction(
            ajax = true,
            name = {"测试", "en:Validate"},
            info = {"验证远程服务器连接配置的有效性。", "en:Validate the remote server connection configuration."})
    public void validate(Request request, Response response) throws Exception {
        Map<String, String> data = getDataStore().getDataById(request.getId());
        SSHConfig sshConfig = new SSHConfig()
                .setHostname(data.get("host"))
                .setPort(Integer.valueOf(data.get("sshPort")))
                .setUsername(data.get("sshUserName"))
                .setPassword(data.get("sshPassword"));

        try {
            appContext.getService(SSHService.class).createSSHClient(sshConfig).execCmd("echo");
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMsg(appContext.getI18n(request.getLang(), "validator.fail"));
        }
    }

    @ModelAction(
            name = {"列表", "en:List"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void list(Request request, Response response) throws Exception {
        int pageSize = 10;
        int pageNum = 1;
        int totalSize = 0;

        String pageNumParam = request.getParameter(Listable.PARAMETER_PAGE_NUM);
        if (pageNumParam != null && !pageNumParam.isEmpty()) {
            try {
                pageNum = Integer.parseInt(pageNumParam);
            } catch (NumberFormatException ignored) {
                // 忽略异常，使用默认页码
            }
        }

        response.setPageSize(pageSize);
        response.setPageNum(pageNum);

        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = pageNum * pageSize;

        int index = 0;
        List<Map<String, String>> allData = getDataStore().getAllData();
        for (Map node : allData) {
            if (index >= startIndex && index < endIndex) {
                response.addData(node);
            }
            index++;
            if (index == endIndex) {
                break;
            }
        }
        totalSize += allData.size();
        response.setTotalSize(totalSize);
    }

    @Override
    public DataStore getDataStore() {
        return NODE_DATA_STORE;
    }

    public static final DataStore NODE_DATA_STORE = new Node.NodeDataStore();

    private static class NodeDataStore implements DataStore {

        @Override
        public List<Map<String, String>> getAllData() throws Exception {
            List<Map<String, String>> data = new ArrayList<>();
            qingzhou.config.Node[] nodes = MasterApp.getService(Config.class).getNode();
            if (nodes != null) {
                for (qingzhou.config.Node node : nodes) {
                    data.add(Utils.getPropertiesFromObj(node));
                }
            }
            return data;
        }

        @Override
        public void addData(String id, Map<String, String> properties) throws Exception {
            Config config = MasterApp.getService(Config.class);
            qingzhou.config.Node node = new qingzhou.config.Node();
            Utils.setPropertiesToObj(node, properties);
            config.addNode(node);
        }

        @Override
        public void updateDataById(String id, Map<String, String> data) throws Exception {
            Config config = MasterApp.getService(Config.class);
            qingzhou.config.Node node = new qingzhou.config.Node();
            Utils.setPropertiesToObj(node, data);
            config.deleteNode(id);
            config.addNode(node);
        }

        @Override
        public void deleteDataById(String id) throws Exception {
            Config config = MasterApp.getService(Config.class);
            config.deleteNode(id);
        }
    }
}
