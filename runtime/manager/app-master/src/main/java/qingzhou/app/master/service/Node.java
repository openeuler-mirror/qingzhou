package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.app.master.ConsoleDataStore;
import qingzhou.app.master.MasterApp;
import qingzhou.deployer.App;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(name = App.SYS_MODEL_NODE, icon = "node",
        menuName = "Service", menuOrder = 2,
        nameI18n = {"节点", "en:Node"},
        infoI18n = {"节点是对物理或虚拟计算机环境的抽象，是运行实例的基础设施。",
                "en:A node is an abstraction of a physical or virtual computer environment and is the infrastructure that runs instances."})
public class Node extends ModelBase implements Createable {
    @ModelField(
            shownOnList = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"唯一标识。", "en:Unique identifier."})
    @FieldValidation(required = true, unsupportedStrings = App.SYS_NODE_LOCAL)
    public String id;

    @ModelField(shownOnList = true,
            nameI18n = {"IP", "en:IP"},
            infoI18n = {"连接节点的 IP 地址。", "en:The IP address of the connected node."})
    @FieldValidation(required = true, hostname = true, cannotUpdate = true)
    public String ip;

    @ModelField(shownOnList = true,
            nameI18n = {"管理端口", "en:Management Port"},
            infoI18n = {"节点的管理端口。", "en:The management port of the node."})
    @FieldValidation(required = true, port = true)
    @FieldView(type = FieldType.number)
    public int port = 7000;

    @ModelField(shownOnList = true,
            nameI18n = {"运行中", "en:Running"}, infoI18n = {"了解该组件的运行状态。", "en:Know the operational status of the component."})
    @FieldView(type = FieldType.bool)
    @FieldValidation(cannotAdd = true, cannotUpdate = true)
    public boolean running;

    @ModelAction(name = App.SYS_ACTION_MANAGE_PAGE,
            nameI18n = {"管理", "en:Manage"},
            infoI18n = {"转到此节点的管理页面。", "en:Go to the administration page for this node."})
    @ActionView(icon = "location-arrow", forwardTo = "sys/" + App.SYS_ACTION_MANAGE_PAGE, shownOnList = 1)
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
