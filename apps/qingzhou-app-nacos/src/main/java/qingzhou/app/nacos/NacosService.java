package qingzhou.app.nacos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.*;

@Model(code = "nacos-service", order = 2,
        name = {"服务管理", "en:Service Management"},
        info = {"Nacos服务注册与发现管理", "en:Nacos Service Discovery Management"},
        icon = "Cpu")
public class NacosService extends NacosModelBase implements Page, Show, Add, Delete {

    @ModelField(id = true,
            name = {"服务名称", "en:Service Name"},
            list = true,
            show = true,
            add = true,
            required = true)
    public String id;

    @ModelField(
            name = {"服务名称", "en:Service Name"},
            list = true,
            search = true)
    public String serviceName;

    @ModelField(
            name = {"实例数", "en:Instance Count"},
            list = true,
            show = true)
    public String instanceCount;

    @ModelField(
            name = {"健康实例数", "en:Healthy Count"},
            list = true,
            show = true)
    public String healthyCount;

    @ModelField(
            name = {"IP", "en:IP"},
            list = true,
            show = true,
            add = true)
    public String ip;

    @ModelField(
            name = {"端口", "en:Port"},
            list = true,
            show = true,
            add = true,
            input_type = InputType.number,
            min = 1,
            max = 65535)
    public Integer port;

    @ModelField(
            name = {"权重", "en:Weight"},
            list = true,
            show = true,
            add = true,
            input_type = InputType.decimal,
            min = 0,
            max = 1)
    public Double weight = 1.0;

    @ModelField(
            name = {"是否健康", "en:Healthy"},
            list = true,
            show = true,
            input_type = InputType.bool)
    public Boolean healthy;

    @ModelField(
            name = {"命名空间", "en:Namespace"},
            list = true,
            show = true,
            add = true)
    public String namespaceId = NacosConstants.PUBLIC_NAMESPACE;

    @ModelField(
            name = {"集群", "en:Cluster"},
            list = true,
            show = true,
            add = true)
    public String clusterName = NacosConstants.DEFAULT_CLUSTER;

    @ModelField(
            name = {"元数据", "en:Metadata"},
            show = true,
            input_type = InputType.textarea)
    public String metadata;

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        List<String[]> result = new ArrayList<>();
        
        try {
            NacosApi nacosApi = getNacosApi();
            Map<String, Object> data = nacosApi.getServiceList(pageNum, pageSize);
            List<?> services = (List<?>) data.get("doms");
            
            if (services != null) {
                for (Object serviceObj : services) {
                    String serviceName = (String) serviceObj;
                    Map<String, String> serviceInfo = new HashMap<>();
                    serviceInfo.put("id", serviceName);
                    serviceInfo.put("serviceName", serviceName);

                    Map<String, Object> instanceData = nacosApi.getServiceInstances(serviceName);
                    List<?> instances = (List<?>) instanceData.get("hosts");
                    
                    int instanceCount = instances != null ? instances.size() : 0;
                    int healthyCount = 0;
                    
                    if (instances != null && !instances.isEmpty()) {
                        Map<String, Object> firstInstance = (Map<String, Object>) instances.get(0);
                        serviceInfo.put("ip", String.valueOf(firstInstance.get("ip")));
                        serviceInfo.put("port", String.valueOf(firstInstance.get("port")));
                        serviceInfo.put("healthy", String.valueOf(firstInstance.get("healthy")));
                        serviceInfo.put("namespaceId", String.valueOf(firstInstance.get("namespaceId")));
                        serviceInfo.put("clusterName", String.valueOf(firstInstance.get("clusterName")));
                        
                        for (Object instance : instances) {
                            Map<String, Object> inst = (Map<String, Object>) instance;
                            if (Boolean.TRUE.equals(inst.get("healthy"))) {
                                healthyCount++;
                            }
                        }
                    }
                    
                    serviceInfo.put("instanceCount", String.valueOf(instanceCount));
                    serviceInfo.put("healthyCount", String.valueOf(healthyCount));
                    
                    String[] row = new String[listFields.length];
                    for (int j = 0; j < listFields.length; j++) {
                        row[j] = serviceInfo.getOrDefault(listFields[j], "");
                    }
                    result.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }

    @Override
    public int totalSize(Map<String, String> query) {
        try {
            NacosApi nacosApi = getNacosApi();
            Map<String, Object> data = nacosApi.getServiceList(1, 1);
            return ((Number) data.get("count")).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean contains(String id) {
        try {
            NacosApi nacosApi = getNacosApi();
            Map<String, Object> data = nacosApi.getServiceInstances(id);
            return data != null;
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
            Map<String, Object> instanceData = nacosApi.getServiceInstances(id);
            List<?> instances = (List<?>) instanceData.get("hosts");
            
            if (instances != null && !instances.isEmpty()) {
                Map<String, Object> firstInstance = (Map<String, Object>) instances.get(0);
                result.put("ip", String.valueOf(firstInstance.get("ip")));
                result.put("port", String.valueOf(firstInstance.get("port")));
                result.put("weight", String.valueOf(firstInstance.get("weight")));
                result.put("healthy", String.valueOf(firstInstance.get("healthy")));
                result.put("namespaceId", String.valueOf(firstInstance.get("namespaceId")));
                result.put("clusterName", String.valueOf(firstInstance.get("clusterName")));
                
                Map<String, Object> meta = (Map<String, Object>) firstInstance.get("metadata");
                if (meta != null) {
                    result.put("metadata", getNacosApp().getJson().toJson(meta));
                }
                
                result.put("instanceCount", String.valueOf(instances.size()));
                
                int healthyCount = 0;
                for (Object instance : instances) {
                    Map<String, Object> inst = (Map<String, Object>) instance;
                    if (Boolean.TRUE.equals(inst.get("healthy"))) {
                        healthyCount++;
                    }
                }
                result.put("healthyCount", String.valueOf(healthyCount));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        String serviceName = data.get("id");
        String ip = data.get("ip");
        String portStr = data.get("port");
        String weightStr = data.get("weight");
        int port = Integer.parseInt(portStr);
        double weight = weightStr != null ? Double.parseDouble(weightStr) : NacosConstants.DEFAULT_WEIGHT;
        String namespaceId = getOrDefault(data.get("namespaceId"), NacosConstants.PUBLIC_NAMESPACE);
        String clusterName = getOrDefault(data.get("clusterName"), NacosConstants.DEFAULT_CLUSTER);
        
        try {
            NacosApi nacosApi = getNacosApi();
            boolean success = nacosApi.registerInstance(serviceName, ip, port, weight, namespaceId, clusterName);
            if (!success) {
                handleFailure(NacosConstants.ERROR_SERVICE_REGISTER_FAILED, "返回状态码不是 200");
            }
        } catch (Exception e) {
            handleFailure(NacosConstants.ERROR_SERVICE_REGISTER_FAILED, e);
        }
    }

    @Override
    public void delete(String id) throws Exception {
        try {
            NacosApi nacosApi = getNacosApi();
            boolean success = nacosApi.deleteService(id);
            if (!success) {
                handleFailure(NacosConstants.ERROR_SERVICE_DELETE_FAILED, "返回结果不是 true");
            }
        } catch (Exception e) {
            handleFailure(NacosConstants.ERROR_SERVICE_DELETE_FAILED, e);
        }
    }

    @ModelAction(name = {"查询实例", "en:Query Instances"},
            info = {"查询服务的所有实例", "en:Query all instances of the service"})
    public void queryInstances(Request request) {
        String serviceName = request.getId();
        if (serviceName == null) {
            request.getResponse().msg("服务名称不能为空").msgLevel(Response.MsgLevel.error);
            return;
        }
        
        try {
            NacosApi nacosApi = getNacosApi();
            Map<String, Object> data = nacosApi.getServiceInstances(serviceName);
            request.getResponse().data(data);
        } catch (Exception e) {
            request.getResponse().msg("查询失败: " + e.getMessage()).msgLevel(Response.MsgLevel.error);
        }
    }
}