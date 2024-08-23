//package qingzhou.app.master.service;
//
//import qingzhou.api.FieldType;
//import qingzhou.api.Model;
//import qingzhou.api.ModelAction;
//import qingzhou.api.ModelBase;
//import qingzhou.api.ModelField;
//import qingzhou.api.Request;
//import qingzhou.api.Response;
//import qingzhou.api.type.Listable;
//import qingzhou.app.master.MasterApp;
//import qingzhou.deployer.ResponseImpl;
//import qingzhou.deployer.DeployerConstants;
//import qingzhou.registry.InstanceInfo;
//import qingzhou.registry.Registry;
//
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//@Model(code = DeployerConstants.MASTER_APP_CLUSTER_MODEL_NAME, icon = "sitemap",
//        menu = "Service", order = 3,
//        name = {"集群", "en:Cluster"},
//        info = {"注册到集中管理的 QingZhou 实例集群。",
//                "en:Register with a centrally managed QingZhou instance cluster."})
//public class Cluster extends ModelBase implements Listable {
//    @ModelField(
//            list = true,
//            editable = false,
//            name = {"名称", "en:Name"},
//            info = {"唯一标识。", "en:Unique identifier."})
//    public String id;
//
//    @ModelField(
//            list = true,
//            name = {"别名", "en:Alias"},
//            info = {"用来标识实例集群的备注。", "en:A note used to identify the current instance cluster."})
//    public String alias;
//
//    @ModelField(
//            type = FieldType.number, list = true,
//            createable = false, editable = false,
//            name = {"实例数量", "en:Instance Count"},
//            info = {"集群中包含的实例数量。", "en:Number of instances contained in the cluster."})
//    public int instanceCount;
//
//    @ModelAction(
//            name = {"管理", "en:Manage"}, order = 1, disable = true,// TODO
//            info = {"转到此集群的管理页面。", "en:Go to the administration page for this cluster."})
//    public void manage(Request request, Response response) throws Exception {
//    }
//
//    @ModelAction(
//            disable = true,
//            name = {"编辑", "en:Edit"},
//            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
//    public void edit(Request request, Response response) throws Exception {
//        show(request, response);
//    }
//
//    @ModelAction(
//            name = {"查看", "en:Show"},
//            info = {"查看该组件的详细配置信息。", "en:View the detailed configuration information of the component."})
//    public void show(Request request, Response response) throws Exception {
//        list(request, response);
//        ((ResponseImpl) response).getDataList().removeIf(cluster -> !cluster.get(idFieldName()).equals(request.getId()));
//    }
//
//    @ModelAction(disable = true,
//            name = {"删除", "en:Delete"},
//            info = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
//                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
//    public void delete(Request request, Response response) throws Exception {
//    }
//
//    @ModelAction(
//            name = {"列表", "en:List"},
//            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
//    public void list(Request request, Response response) throws Exception {
//        int pageSize = 10;
//        int pageNum = 1;
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
//        // 设置分页信息
//        response.setPageSize(pageSize);
//        response.setPageNum(pageNum);
//
//        // 计算起始和结束索引
//        int startIndex = (pageNum - 1) * pageSize;
//
//        // 获取注册表中的所有实例
//        Registry registry = MasterApp.getService(Registry.class);
//        Collection<String> allInstanceId = registry.getAllInstanceId();
//
//        // 过滤、分组并进行分页处理
//        Map<String, Long> clusterCounts = allInstanceId.stream()
//                .map(registry::getInstanceInfo)
//                .filter(Objects::nonNull)
//                .filter(instanceInfo -> instanceInfo.getClusterId() != null && !instanceInfo.getClusterId().isEmpty())
//                .collect(Collectors.groupingBy(
//                        InstanceInfo::getClusterId,
//                        Collectors.counting()
//                ));
//
//        // 获取分页后的cluster
//        List<Cluster> clusters = clusterCounts.entrySet().stream()
//                .skip(startIndex)
//                .limit(pageSize)
//                .map(entry -> {
//                    Cluster cluster = new Cluster();
//                    cluster.id = entry.getKey();
//                    cluster.alias = entry.getKey();
//                    cluster.instanceCount = entry.getValue().intValue();
//                    return cluster;
//                })
//                .collect(Collectors.toList());
//
//        for (Cluster cluster : clusters) {
//            response.addModelData(cluster);
//        }
//
//        response.setTotalSize(clusterCounts.size());
//    }
//}
