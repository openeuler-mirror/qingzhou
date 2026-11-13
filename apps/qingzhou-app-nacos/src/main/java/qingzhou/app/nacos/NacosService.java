package qingzhou.app.nacos;

import java.util.*;

import qingzhou.api.*;
import qingzhou.api.action.*;
import qingzhou.api.annotations.*;
import qingzhou.json.Json;

@Model(code = "nacos-service", order = 2, icon = "Cpu",
        name = {"服务管理", "en:Service Management"}, info = {"Nacos服务注册与发现管理", "en:Nacos Service Discovery Management"})
public class NacosService extends NacosModelBase implements Page, Show, Add, Delete {

    @ModelField(id = true, list = true, show = true, add = true, required = true, name = {"服务名称", "en:Service Name"})
    public String id;
    @ModelField(name = {"服务名称", "en:Service Name"}, list = true, search = true)
    public String serviceName;
    @ModelField(name = {"实例数", "en:Instance Count"}, list = true, show = true)
    public String instanceCount;
    @ModelField(name = {"健康实例数", "en:Healthy Count"}, list = true, show = true)
    public String healthyCount;
    @ModelField(name = {"IP", "en:IP"}, list = true, show = true, add = true)
    public String ip;
    @ModelField(name = {"端口", "en:Port"}, list = true, show = true, add = true, input_type = InputType.number, min_value = 1, max_value = 65535)
    public Integer port;
    @ModelField(name = {"权重", "en:Weight"}, list = true, show = true, add = true, input_type = InputType.decimal, min_value = 0, max_value = 1)
    public Double weight = NacosConstants.DEFAULT_WEIGHT;
    @ModelField(name = {"是否健康", "en:Healthy"}, list = true, show = true, input_type = InputType.bool)
    public Boolean healthy;
    @ModelField(name = {"命名空间", "en:Namespace"}, list = true, show = true, add = true)
    public String namespaceId = NacosConstants.PUBLIC_NAMESPACE;
    @ModelField(name = {"集群", "en:Cluster"}, list = true, show = true, add = true)
    public String clusterName = NacosConstants.DEFAULT_CLUSTER;
    @ModelField(name = {"元数据", "en:Metadata"}, show = true, input_type = InputType.textarea)
    public String metadata;

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) {
        try {
            NacosApi nacosApi = getNacosApi();
            List<?> services = (List<?>) nacosApi.getServiceList(pageNum, pageSize).get("doms");
            List<Map<String, String>> items = new ArrayList<>();
            if (services != null) {
                for (Object serviceObj : services) {
                    String serviceName = (String) serviceObj;
                    Map<String, String> info = new HashMap<>();
                    info.put("id", serviceName);
                    info.put("serviceName", serviceName);

                    List<?> instances = (List<?>) nacosApi.getServiceInstances(serviceName).get("hosts");
                    int instanceCount = instances != null ? instances.size() : 0;
                    int healthyCount = 0;
                    if (instances != null && !instances.isEmpty()) {
                        Map<String, Object> first = (Map<String, Object>) instances.get(0);
                        info.put("ip", String.valueOf(first.get("ip")));
                        info.put("port", String.valueOf(first.get("port")));
                        info.put("healthy", String.valueOf(first.get("healthy")));
                        info.put("namespaceId", String.valueOf(first.get("namespaceId")));
                        info.put("clusterName", String.valueOf(first.get("clusterName")));
                        for (Object instance : instances) {
                            Map<String, Object> inst = (Map<String, Object>) instance;
                            if (Boolean.TRUE.equals(inst.get("healthy"))) healthyCount++;
                        }
                    }
                    info.put("instanceCount", String.valueOf(instanceCount));
                    info.put("healthyCount", String.valueOf(healthyCount));
                    items.add(info);
                }
            }
            return toRows(items, listFields);
        } catch (Exception e) {
            logError("Nacos service query failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public int total(Map<String, String> query) {
        try {
            return ((Number) getNacosApi().getServiceList(1, 1).get("count")).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean contains(String id) {
        try {
            return getNacosApi().getServiceInstances(id) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, String> show(String id) {
        Map<String, String> result = new HashMap<>();
        result.put("id", id);
        result.put("serviceName", id);
        try {
            NacosApi nacosApi = getNacosApi();
            List<?> instances = (List<?>) nacosApi.getServiceInstances(id).get("hosts");
            if (instances != null && !instances.isEmpty()) {
                Map<String, Object> first = (Map<String, Object>) instances.get(0);
                result.put("ip", String.valueOf(first.get("ip")));
                result.put("port", String.valueOf(first.get("port")));
                result.put("weight", String.valueOf(first.get("weight")));
                result.put("healthy", String.valueOf(first.get("healthy")));
                result.put("namespaceId", String.valueOf(first.get("namespaceId")));
                result.put("clusterName", String.valueOf(first.get("clusterName")));

                Map<String, Object> meta = (Map<String, Object>) first.get("metadata");
                if (meta != null) result.put("metadata", getAppContext().getService(Json.class).toJson(meta));
                result.put("instanceCount", String.valueOf(instances.size()));

                int healthyCount = 0;
                for (Object instance : instances) {
                    Map<String, Object> inst = (Map<String, Object>) instance;
                    if (Boolean.TRUE.equals(inst.get("healthy"))) healthyCount++;
                }
                result.put("healthyCount", String.valueOf(healthyCount));
            }
        } catch (Exception e) {
            logError("Nacos service query failed", e);
        }
        return result;
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        String serviceName = data.get("id");
        String ip = data.get("ip");
        int port = Integer.parseInt(data.get("port"));
        String weightStr = data.get("weight");
        double weight = weightStr != null ? Double.parseDouble(weightStr) : NacosConstants.DEFAULT_WEIGHT;
        String namespaceId = getOrDefault(data.get("namespaceId"), NacosConstants.PUBLIC_NAMESPACE);
        String clusterName = getOrDefault(data.get("clusterName"), NacosConstants.DEFAULT_CLUSTER);
        writeOrFail(NacosConstants.ERROR_SERVICE_REGISTER_FAILED, "返回状态码不是 200",
                () -> getNacosApi().registerInstance(serviceName, ip, port, weight, namespaceId, clusterName));
    }

    @Override
    public void delete(String id) throws Exception {
        writeOrFail(NacosConstants.ERROR_SERVICE_DELETE_FAILED, "返回结果不是 true",
                () -> getNacosApi().deleteService(id));
    }

    @ModelAction(name = {"查询实例", "en:Query Instances"}, info = {"查询服务的所有实例", "en:Query all instances of the service"})
    public void queryInstances(Request request) {
        String serviceName = request.getId();
        if (serviceName == null) {
            request.getResponse().message("服务名称不能为空").messageLevel(Response.MessageLevel.error);
            return;
        }
        try {
            request.getResponse().data(getNacosApi().getServiceInstances(serviceName));
        } catch (Exception e) {
            request.getResponse().message("查询失败: " + e.getMessage()).messageLevel(Response.MessageLevel.error);
        }
    }
}
