package qingzhou.app.system.service;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Addable;
import qingzhou.app.system.Main;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Model(code = "node", icon = "node",
        menu = Main.SERVICE_MENU, order = 3,
        name = {"节点", "en:Node"},
        info = {"节点表示一种物理环境，通常是基于 SSH 协议进行管理，轻舟在节点之上运行轻舟的实例。",
                "en:A node represents a physical environment, usually managed based on the SSH protocol, and Qingzhou runs an instance of Qingzhou on top of the node."})
public class Node extends ModelBase implements Addable {
    @ModelField(
            required = true,
            list = true,
            name = {"名称", "en:Name"},
            info = {"表示本条数据的 ID。", "en:Indicates the ID of this piece of data."})
    public String id;

    @ModelField(
            required = true,
            list = true,
            name = {"SSH 主机", "en:SSH Address"},
            info = {"连接节点的 IP 地址。", "en:The IP address of the connected node."})
    public String host;

    @ModelField(
            type = FieldType.number,
            required = true,
            port = true,
            list = true,
            name = {"SSH 端口", "en:SSH Port"},
            info = {"连接节点的 IP 地址的ssh服务使用的端口。", "en:Port used by the SSH service to connect the IP address of the node."})
    public Integer sshPort = 22;

    @ModelField(
            required = true,
            name = {"SSH 用户名", "en:SSH User Name "},
            info = {"SSH 登录使用的用户名。", "en:User name for SSH login"})
    public String sshUserName;

    @ModelField(
            type = FieldType.password,
            required = true,
            name = {"SSH 密码", "en:SSH Password"},
            info = {"SSH 登录使用的密码", "en:Password or certificate."})
    public String sshPassword;

    @Override
    public void start() {
        appContext.addI18n("validator.fail", new String[]{"连接失败，请检查配置信息是否正确", "en:The connection fails. Please check whether the configuration information is correct"});
    }

    @Override
    public void addData(Map<String, String> data) throws Exception {

    }

    @Override
    public void deleteData(String id) throws Exception {

    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {

    }

    @Override
    public Map<String, String> showData(String id) throws Exception {
        return Collections.emptyMap();
    }

// todo   @ModelAction(
//            name = {"添加", "en:Add"},
//            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
//    public void add(Request request) throws Exception {
//        if (getDataStore().exists(request.getParameter(idFieldName()))) {
//            response.setSuccess(false);
//            response.setMsg(appContext.getI18n(request.getLang(), "validator.exist"));
//            return;
//        }
//        Map<String, String> newNode = request.getParameters();
//        getDataStore().addData(newNode.get(idFieldName()), newNode);
//    }
//
//    @ModelAction(
//            name = {"编辑", "en:Edit"},
//            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
//    public void edit(Request request) throws Exception {
//        show(request, response);
//    }
//
//    @ModelAction(
//            name = {"查看", "en:Show"},
//            info = {"查看该组件的相关信息。", "en:View the information of this model."})
//    public void show(Request request) throws Exception {
//        DataStore dataStore = getDataStore();
//        Map<String, String> data = dataStore.getDataById(request.getId());
//        response.addData(data);
//    }
//
//    @ModelAction(
//            name = {"更新", "en:Update"},
//            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
//    public void update(Request request) throws Exception {
//        DataStore dataStore = getDataStore();
//        Map<String, String> newData = request.getParameters();
//        dataStore.updateDataById(request.getId(), newData);
//    }
//
//    @ModelAction(
//            name = {"删除", "en:Delete"},
//            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
//                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
//    public void delete(Request request) throws Exception {
//        String id = request.getId();
//        DataStore dataStore = getDataStore();
//        dataStore.deleteDataById(id);
//    }
//
//    @ModelAction(
//            ajax = true,
//            name = {"测试", "en:Validate"},
//            info = {"验证远程服务器连接配置的有效性。", "en:Validate the remote server connection configuration."})
//    public void validate(Request request) throws Exception {
//        Map<String, String> data = getDataStore().getDataById(request.getId());
//        SSHConfig sshConfig = new SSHConfig()
//                .setHostname(data.get("host"))
//                .setPort(Integer.valueOf(data.get("sshPort")))
//                .setUsername(data.get("sshUserName"))
//                .setPassword(data.get("sshPassword"));
//
//        try {
//            appContext.getService(SSHService.class).createSSHClient(sshConfig).execCmd("echo");
//        } catch (Exception e) {
//            response.setSuccess(false);
//            response.setMsg(appContext.getI18n(request.getLang(), "validator.fail"));
//        }
//    }
//
//    @ModelAction(
//            name = {"列表", "en:List"},
//            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
//    public void list(Request request) throws Exception {
//        int pageSize = 10;
//        int pageNum = 1;
//        int totalSize = 0;
//
//        String pageNumParam = request.getParameter("pageNum");
//        if (pageNumParam != null && !pageNumParam.isEmpty()) {
//            try {
//                pageNum = Integer.parseInt(pageNumParam);
//            } catch (NumberFormatException ignored) {
//                // 忽略异常，使用默认页码
//            }
//        }
//
//        response.setPageSize(pageSize);
//        response.setPageNum(pageNum);
//
//        int startIndex = (pageNum - 1) * pageSize;
//        int endIndex = pageNum * pageSize;
//
//        int index = 0;
//        List<Map<String, String>> allData = getDataStore().getAllData();
//        for (Map node : allData) {
//            if (index >= startIndex && index < endIndex) {
//                response.addData(node);
//            }
//            index++;
//            if (index == endIndex) {
//                break;
//            }
//        }
//        totalSize += allData.size();
//        response.setTotalSize(totalSize);
//    }
//
//    private static class NodeDataStore implements DataStore {
//
//        @Override
//        public List<Map<String, String>> getAllData() throws Exception {
//            List<Map<String, String>> data = new ArrayList<>();
//            qingzhou.config.Node[] nodes = MasterApp.getService(Config.class).getNode();
//            if (nodes != null) {
//                for (qingzhou.config.Node node : nodes) {
//                    data.add(Utils.getPropertiesFromObj(node));
//                }
//            }
//            return data;
//        }
//
//        @Override
//        public void addData(String id, Map<String, String> properties) throws Exception {
//            Config config = MasterApp.getService(Config.class);
//            qingzhou.config.Node node = new qingzhou.config.Node();
//            Utils.setPropertiesToObj(node, properties);
//            config.addNode(node);
//        }
//
//        @Override
//        public void updateDataById(String id, Map<String, String> data) throws Exception {
//            Config config = MasterApp.getService(Config.class);
//            qingzhou.config.Node node = new qingzhou.config.Node();
//            Utils.setPropertiesToObj(node, data);
//            config.deleteNode(id);
//            config.addNode(node);
//        }
//
//        @Override
//        public void deleteDataById(String id) throws Exception {
//            Config config = MasterApp.getService(Config.class);
//            config.deleteNode(id);
//        }
//    }
}
