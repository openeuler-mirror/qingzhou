package qingzhou.app.master.service;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Listable;

import java.util.HashMap;
import java.util.Map;

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
            name = {"管理", "en:Manage"}, show = "running=true", order = 1,
            info = {"转到此实例的管理页面。", "en:Go to the administration page for this instance."})
    public void switchTarget(Request request, Response response) throws Exception {
    }

    @ModelAction(
            name = {"列表", "en:List"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void list(Request request, Response response) throws Exception {
        response.setTotalSize(1);
        response.setPageSize(10);
        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getParameter(Listable.PARAMETER_PAGE_NUM));
        } catch (NumberFormatException ignored) {
        }
        response.setPageNum(pageNum);

        Map<String, String> local = new HashMap<>();
        local.put("id", "local");
        local.put("ip", "127.0.0.1");
        local.put("port", "7000");
        local.put("running", "true");
        response.addData(local);
    }
}
