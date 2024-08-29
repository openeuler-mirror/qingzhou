//package qingzhou.app.master.service;
//
//import qingzhou.api.*;
//import qingzhou.api.type.Listable;
//import qingzhou.app.master.MasterApp;
//import qingzhou.config.Agent;
//import qingzhou.config.Config;
//import qingzhou.registry.InstanceInfo;
//import qingzhou.registry.Registry;
//
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.Map;
//
//@Model(code = "instance", icon = "stack",
//        menu = "Service", order = 2,
//        name = {"实例", "en:Instance"},
//        info = {"实例是轻舟提供的运行应用的实际环境，即应用的运行时。",
//                "en:The instance is the actual environment for running the application provided by QingZhou, that is, the runtime of the application."})
//public class Instance extends ModelBase implements Listable {
//    @ModelField(
//            required = true,
//            list = true,
//            name = {"名称", "en:Name"},
//            info = {"唯一标识。", "en:Unique identifier."})
//    public String id;

//    @ModelField(list = true,
//            name = {"IP", "en:IP"},
//            info = {"连接实例的 IP 地址。", "en:The IP address of the connected instance."})
//    public String ip;
//
//    @ModelField(list = true,
//            name = {"管理端口", "en:Management Port"},
//            info = {"实例的管理端口。", "en:The management port of the instance."})
//    public int port = 7000;
//
//    @ModelField(list = true, createable = false, editable = false,
//            name = {"运行中", "en:Running"}, info = {"了解该组件的运行状态。", "en:Know the operational status of the component."})
//    public boolean running;
//
//    @Override
//    public void start() {
//        appContext.addActionFilter((request, response) -> {
//            if (request.getId().equals("local")) {
//                if ("update".equals(request.getAction())
//                        || "delete".equals(request.getAction())) {
//                    return appContext.getI18n(request.getLang(), "validator.master.system");
//                }
//            }
//            return null;
//        });
//    }
//
//    @ModelAction(
//            order = -1,
//            name = {"注册检查", "en:Check Registry"},
//            info = {"用于接收实例心跳信息。", "en:Used to receive the heartbeat information of the instance."})
//    public void checkRegistry(Request request) {
//        String fingerprint = request.getParameter("fingerprint");
//        if (fingerprint != null) {
//            Map<String, String> result = new HashMap<>();
//            Registry registry = MasterApp.getService(Registry.class);
//            result.put(fingerprint, String.valueOf(registry.checkRegistry(fingerprint)));
//            response.addData(result);
//        }
//    }
//
//    @ModelAction(
//            order = -1,
//            name = {"注册实例", "en:Register"},
//            info = {"用于接收实例注册的信息。", "en:Information used to receive instance registrations."})
//    public void register(Request request) {
//        String doRegister = request.getParameter("doRegister");
//        if (doRegister != null) {
//            Registry registry = MasterApp.getService(Registry.class);
//            registry.register(doRegister);
//        }
//    }
//
//    @ModelAction(
//            name = {"管理", "en:Manage"}, show = "running=true", order = 1,
//            info = {"转到此实例的管理页面。", "en:Go to the administration page for this instance."})
//    public void manage(Request request) throws Exception {
//    }
//
//    @ModelAction(
//            name = {"查看", "en:Show"},
//            info = {"查看该组件的相关信息。", "en:View the information of this model."})
//    public void show(Request request) {
//        String id = request.getId();
//        if ("local".equals(id)) {
//            response.addData(localInstance());
//            return;
//        }
//        Registry registry = MasterApp.getService(Registry.class);
//        InstanceInfo instanceInfo = registry.getInstanceInfo(id);
//        Map<String, String> instance = new HashMap<>();
//        instance.put("id", id);
//        instance.put("ip", instanceInfo.getHost());
//        instance.put("port", String.valueOf(instanceInfo.getPort()));
//        instance.put("running", Boolean.TRUE.toString());
//        response.addData(instance);
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
//        if (startIndex == 0) {
//            response.addData(localInstance());
//            totalSize = 1;
//        }
//
//        int index = 0;
//        Registry registry = MasterApp.getService(Registry.class);
//        Collection<String> allInstanceId = registry.getAllInstanceId();
//        for (String instanceId : allInstanceId) {
//            InstanceInfo instanceInfo = registry.getInstanceInfo(instanceId);
//            if (instanceInfo != null) {
//                Map<String, String> data = new HashMap<>();
//                data.put("id", instanceInfo.getId());
//                data.put("ip", instanceInfo.getHost());
//                data.put("port", String.valueOf(instanceInfo.getPort()));
//                data.put("running", "true");
//
//                if (index >= startIndex && index < endIndex) {
//                    response.addData(data);
//                }
//                index++;
//
//                if (index == endIndex) {
//                    break;
//                }
//            }
//        }
//        totalSize += allInstanceId.size();
//        response.setTotalSize(totalSize);
//    }
//
//    private Map<String, String> localInstance() {
//        Map<String, String> local = new HashMap<>();
//        local.put("id", "local");
//        Agent agent = MasterApp.getService(Config.class).getAgent();
//        local.put("ip", agent.getAgentHost());
//        local.put("port", String.valueOf(agent.getAgentPort()));
//        local.put("running", Boolean.TRUE.toString());
//        return local;
//    }
//}
