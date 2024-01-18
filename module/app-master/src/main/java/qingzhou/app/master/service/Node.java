package qingzhou.app.master.service;

import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.framework.console.ConsoleConstants;

import java.util.HashMap;
import java.util.Map;

@Model(name = "node", icon = "node",
        menuName = "Service",
        nameI18n = {"节点", "en:Node"},
        infoI18n = {"节点是对物理或虚拟计算机环境的抽象，是运行实例的基础设施。",
                "en:A node is an abstraction of a physical or virtual computer environment and is the infrastructure that runs instances."})
public class Node extends ModelBase implements AddModel {
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
    public int port = 9060;

    @ModelField(showToList = true, disableOnCreate = true, disableOnEdit = true,
            type = FieldType.bool,
            nameI18n = {"运行中", "en:Running"}, infoI18n = {"了解该组件的运行状态。", "en:Know the operational status of the component."})
    public boolean running;

    @Override
    public void list(Request request, Response response) throws Exception {
        Map<String, String> node = new HashMap<>();
        node.put("id", ConsoleConstants.LOCAL_NODE_NAME);
        node.put("ip", "0.0.0.0");
        node.put("port", "9060");
        node.put("running", "true");
        response.addData(node);
        response.setPageNum(1);
    }
}
