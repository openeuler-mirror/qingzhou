package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.app.master.ConsoleDataStore;
import qingzhou.app.master.Main;
import qingzhou.framework.app.App;
import qingzhou.framework.config.Config;
import qingzhou.framework.util.StringUtil;

import java.util.*;

@Model(name = App.SYS_MODEL_NODE, icon = "node",
        menuName = "Service", menuOrder = 2,
        nameI18n = {"节点", "en:Node"},
        infoI18n = {"节点是对物理或虚拟计算机环境的抽象，是运行实例的基础设施。",
                "en:A node is an abstraction of a physical or virtual computer environment and is the infrastructure that runs instances."})
public class Node extends ModelBase implements AddModel {

    @Override
    public void init() {
        getAppContext().getConsoleContext().addI18N("node.id.system", new String[]{"该名称已被系统占用，请更换为其它名称", "en:This name is already occupied by the system, please replace it with another name"});
    }

    @ModelField(
            required = true, showToList = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"唯一标识。", "en:Unique identifier."})
    public String id;

    @ModelField(required = true, showToList = true,
            isIpOrHostname = true, disableOnEdit = true,
            nameI18n = {"IP", "en:IP"},
            infoI18n = {"连接节点的 IP 地址。", "en:The IP address of the connected node."})
    public String ip;

    @ModelField(showToList = true, type = FieldType.number,
            required = true,
            isPort = true,
            nameI18n = {"管理端口", "en:Management Port"},
            infoI18n = {"节点的管理端口。", "en:The management port of the node."})
    public int port = 7000;

    @ModelField(showToList = true, disableOnCreate = true, disableOnEdit = true,
            type = FieldType.bool,
            nameI18n = {"运行中", "en:Running"}, infoI18n = {"了解该组件的运行状态。", "en:Know the operational status of the component."})
    public boolean running;

    @Override
    public String validate(Request request, String fieldName) {
        if (fieldName.equals("id")) {
            if (request.getParameter("id").equals(App.SYS_NODE_LOCAL)) {
                return "node.id.system";
            }
        }

        return null;
    }

    @ModelAction(name = qingzhou.framework.app.App.SYS_ACTION_MANAGE,
            icon = "location-arrow", forwardToPage = "sys/" + qingzhou.framework.app.App.SYS_ACTION_MANAGE,
            nameI18n = {"管理", "en:Manage"}, showToList = true, orderOnList = -1,
            infoI18n = {"转到此节点的管理页面。", "en:Go to the administration page for this node."})
    public void switchTarget(Request request, Response response) throws Exception {
    }

    @Override
    public void list(Request request, Response response) throws Exception {
        String modelName = request.getModelName();
        DataStore dataStore = getDataStore();
        if (dataStore == null) {
            return;
        }
        int totalSize = dataStore.getTotalSize(modelName);
        response.setTotalSize(totalSize);

        int pageSize = pageSize();
        response.setPageSize(pageSize);

        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getParameter(PARAMETER_PAGE_NUM));
        } catch (NumberFormatException ignored) {
        }
        response.setPageNum(pageNum);

        String userName = request.getUserName();
        if (!"qingzhou".equals(userName)) {
            Map<String, String> user = dataStore.getDataById("user", userName);
            String nodeNames = user.get("nodes");
            if (StringUtil.notBlank(nodeNames)) {
                String[] nodes = nodeNames.split(",");
                List<String> expressions = new ArrayList<>();
                for (String node : nodes) {
                    expressions.add("@id='" + node + "'");
                }
                modelName = modelName + "[" + String.join(" or ", expressions) + "]";
            }
        }

        String[] dataIdInPage = dataStore.getDataIdInPage(modelName, pageSize, pageNum).toArray(new String[0]);
        ModelManager manager = getAppContext().getConsoleContext().getModelManager();
        String finalModelName = modelName;
        String[] fieldNamesToList = Arrays.stream(manager.getFieldNames(modelName)).filter(s -> manager.getModelField(finalModelName, s).showToList()).toArray(String[]::new);
        List<Map<String, String>> result = dataStore.getDataFieldByIds(modelName, dataIdInPage, fieldNamesToList);
        for (Map<String, String> data : result) {
            response.addData(data);
        }
    }

    @Override
    public DataStore getDataStore() {
        if (dataStore == null) {
            dataStore = new NodeDataStore();
        }

        return dataStore;
    }

    private DataStore dataStore;

    private static class NodeDataStore extends ConsoleDataStore {
        private final Map<String, String> localNode;

        NodeDataStore() {
            localNode = new HashMap<>();
            localNode.put("id", App.SYS_NODE_LOCAL);
            localNode.put("ip", "127.0.0.1");
            localNode.put("port", Main.getService(Config.class).getConfig("//console").get("port"));
            localNode.put("running", "true");
        }

        @Override
        public List<Map<String, String>> getAllData(String type) {
            List<Map<String, String>> allData = super.getAllData(type);
            allData.add(0, localNode);
            return allData;
        }
    }

}
