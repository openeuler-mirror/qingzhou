package qingzhou.app.master.service;

import qingzhou.api.*;
import qingzhou.api.type.Createable;
import qingzhou.api.type.Deletable;
import qingzhou.api.type.Editable;
import qingzhou.api.type.Listable;
import qingzhou.app.master.MasterApp;
import qingzhou.config.Agent;
import qingzhou.config.Config;
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
    private static final Map<String, String> local = new HashMap<>();

    static {
        local.put("id", DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID);
        Agent agent = MasterApp.getService(Config.class).getAgent();
        local.put("ip", agent.getHost());
        local.put("port", String.valueOf(agent.getPort()));
        local.put("running", Boolean.TRUE.toString());
    }

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

    @ModelField(list = true, createable = false, editable = false,
            name = {"运行中", "en:Running"}, info = {"了解该组件的运行状态。", "en:Know the operational status of the component."})
    public boolean running;

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
            name = {"管理", "en:Manage"}, show = "running=true", order = 1,
            info = {"转到此实例的管理页面。", "en:Go to the administration page for this instance."})
    public void manage(Request request, Response response) throws Exception {
    }

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        String id = request.getId();
        if (DeployerConstants.MASTER_APP_DEFAULT_INSTANCE_ID.equals(id)) {
            response.addData(local);
            return;
        }
        Registry registry = MasterApp.getService(Registry.class);
        InstanceInfo instanceInfo = registry.getInstanceInfo(id);
        Map<String, String> instance = new HashMap<>();
        instance.put("id", id);
        instance.put("ip", instanceInfo.getHost());
        instance.put("port", String.valueOf(instanceInfo.getPort()));
        instance.put("running", Boolean.TRUE.toString());
        response.addData(instance);
    }

    @ModelAction(disable = true,
            name = {"创建", "en:Create"},
            info = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    public void create(Request request, Response response) throws Exception {
    }

    @ModelAction(disable = true,
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request, Response response) throws Exception {
    }

    @ModelAction(
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        show(request, response);
    }

    @ModelAction(disable = true,
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
    }

    @ModelAction(disable = true,
            name = {"删除", "en:Delete"},
            info = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
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
