package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Editable;
import qingzhou.api.type.Listable;
import qingzhou.app.master.MasterApp;
import qingzhou.deployer.DeployerConstants;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Model(code = DeployerConstants.MASTER_APP_INSTANCE_MODEL_NAME, icon = "stack",
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
    public void manage(Request request, Response response) throws Exception {
    }

    @Override
    public void start() {
        actionFilters.add((request, response) -> {
            if (request.getId().equals(DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID)) {
                if (Editable.ACTION_NAME_UPDATE.equals(request.getAction())
                        || Deletable.ACTION_NAME_DELETE.equals(request.getAction())) {
                    return appContext.getI18n(request.getLang(), "validator.master.system");
                }
            }
            return null;
        });
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

        if (startIndex == 0) {
            Map<String, String> local = new HashMap<>();
            local.put("id", DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID);
            local.put("ip", "127.0.0.1"); // todo
            local.put("port", "7000"); // todo
            local.put("running", Boolean.TRUE.toString());
            response.addData(local);
            totalSize = 1;
        }

        int index = 0;
        Registry registry = MasterApp.getService(Registry.class);
        Collection<String> allInstanceId = registry.getAllInstanceId();
        for (String instanceId : allInstanceId) {
            InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
            if (instanceInfo != null) {
                Map<String, String> data = new HashMap<>();
                data.put("id", instanceInfo.getId());
                data.put("ip", instanceInfo.getHost());
                data.put("port", String.valueOf(instanceInfo.getPort()));
                data.put("running", "true");

                if (index >= startIndex && index < endIndex) {
                    response.addData(data);
                }
                index++;

                if (index == endIndex) {
                    break;
                }
            }
        }
        totalSize += allInstanceId.size();
        response.setTotalSize(totalSize);
    }
}
