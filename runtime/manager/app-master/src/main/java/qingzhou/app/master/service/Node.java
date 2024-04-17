package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.app.master.ConsoleDataStore;
import qingzhou.app.master.MasterApp;
import qingzhou.deployer.App;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(code = App.SYS_MODEL_NODE, icon = "node",
        menu = "Service", order = 2,
        name = {"节点", "en:Node"},
        info = {"节点是对物理或虚拟计算机环境的抽象，是运行实例的基础设施。",
                "en:A node is an abstraction of a physical or virtual computer environment and is the infrastructure that runs instances."})
public class Node extends ModelBase implements Createable {
    @ModelField(
            list = true,
            name = {"名称", "en:Name"},
            info = {"唯一标识。", "en:Unique identifier."})
    public String id;

    @ModelField(list = true,
            name = {"IP", "en:IP"},
            info = {"连接节点的 IP 地址。", "en:The IP address of the connected node."})
    public String ip;

    @ModelField(list = true,
            name = {"管理端口", "en:Management Port"},
            info = {"节点的管理端口。", "en:The management port of the node."})
    public int port = 7000;

    @ModelField(list = true,
            name = {"运行中", "en:Running"}, info = {"了解该组件的运行状态。", "en:Know the operational status of the component."})
    public boolean running;

    @ModelAction(name = App.SYS_ACTION_MANAGE_PAGE,
            name = {"管理", "en:Manage"},
            info = {"转到此节点的管理页面。", "en:Go to the administration page for this node."})
    public void switchTarget(Request request, Response response) throws Exception {
    }

    @Override
    public DataStore getDataStore() {
        return new NodeDataStore();
    }

    private DataStore dataStore;

    private static class NodeDataStore extends ConsoleDataStore {
        private final Map<String, String> localNode;

        NodeDataStore() {
            localNode = new HashMap<>();
            localNode.put("id", App.SYS_NODE_LOCAL);
            localNode.put("ip", "127.0.0.1");
            try {
                localNode.put("port", MasterApp.getService(Config.class).getDataById("console", null).get("port"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            localNode.put("running", "true");
        }

        @Override
        public List<Map<String, String>> getAllData(String type) throws Exception {
            List<Map<String, String>> allData = super.getAllData(type);
            allData.add(0, localNode);

            // todo 在这里实现数据权限？
//            String userName = request.getUserName();
//            if (!Constants.DEFAULT_ADMINISTRATOR.equals(userName)) {
//                Map<String, String> user = dataStore.getDataById("user", userName);
//                String nodeNames = user.get("nodes");
//                if (StringUtil.notBlank(nodeNames)) {
//                    String[] nodes = nodeNames.split(",");
//                    List<String> expressions = new ArrayList<>();
//                    for (String node : nodes) {
//                        expressions.add("@id='" + node + "'");
//                    }
//                    modelName = modelName + "[" + String.join(" or ", expressions) + "]";
//                }
//            }
//
//            String[] dataIdInPage = dataStore.getDataIdInPage(modelName, pageSize, pageNum).toArray(new String[0]);

            return allData;
        }
    }
}
