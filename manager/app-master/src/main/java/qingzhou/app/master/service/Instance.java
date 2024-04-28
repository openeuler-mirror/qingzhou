package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Createable;

@Model(code = "instance", icon = "node",
        menu = "Service", order = 2,
        name = {"节点", "en:Node"},
        info = {"节点是对物理或虚拟计算机环境的抽象，是运行实例的基础设施。",
                "en:A node is an abstraction of a physical or virtual computer environment and is the infrastructure that runs instances."})
public class Instance extends ModelBase implements Createable {
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

    @ModelAction(
            name = {"管理", "en:Manage"},
            info = {"转到此节点的管理页面。", "en:Go to the administration page for this node."})
    public void switchTarget(Request request, Response response) throws Exception {
    }
}
