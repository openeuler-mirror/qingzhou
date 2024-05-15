package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Createable;

@Model(code = "instance", icon = "stack",
        menu = "Service", order = 2,
        name = {"实例", "en:Instance"},
        info = {"实例是轻舟提供的运行应用的实际环境，即应用的运行时。",
                "en:The instance is the actual environment for running the application provided by Qingzhou, that is, the runtime of the application."})
public class Instance extends ModelBase implements Createable {
    @ModelField(
            list = true,
            name = {"名称", "en:Name"},
            info = {"唯一标识。", "en:Unique identifier."})
    public String id;

    @ModelField(list = true,
            name = {"IP", "en:IP"},
            info = {"连接实例的 IP 地址。", "en:The IP address of the connected instance."})
    public String ip;

    @ModelField(list = true,
            name = {"管理端口", "en:Management Port"},
            info = {"实例的管理端口。", "en:The management port of the instance."})
    public int port = 7000;

    @ModelField(list = true,
            name = {"运行中", "en:Running"}, info = {"了解该组件的运行状态。", "en:Know the operational status of the component."})
    public boolean running;

    @ModelAction(
            name = {"管理", "en:Manage"},
            info = {"转到此实例的管理页面。", "en:Go to the administration page for this instance."})
    public void switchTarget(Request request, Response response) throws Exception {
    }
}
