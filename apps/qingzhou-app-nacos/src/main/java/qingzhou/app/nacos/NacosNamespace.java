package qingzhou.app.nacos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.*;

@Model(code = "nacos-namespace", order = 4,
        name = {"命名空间", "en:Namespace Management"},
        info = {"Nacos命名空间管理", "en:Nacos Namespace Management"},
        icon = "FolderOpened")
public class NacosNamespace extends NacosModelBase implements qingzhou.api.type.List, Show, Add, Update, Delete {

    @ModelField(
            name = {"命名空间名称", "en:Namespace Name"},
            list = true,
            required = true)
    public String namespaceShowName;

    @ModelField(
            name = {"命名空间ID", "en:Namespace ID"},
            list = true,
            id = true)
    public String namespace;

    @ModelField(
            name = {"描述", "en:Description"},
            list = true,
            required = true,
            input_type = InputType.textarea)
    public String namespaceDesc;

    @ModelField(
            name = {"配置数", "en:Config Count"},
            list = true,
            update = false,
            add = false
    )
    public String configCount;

    @Override
    public List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        List<String[]> result = new ArrayList<>();
        
        try {
            NacosApi nacosApi = getNacosApi();
            List<?> namespaces = nacosApi.getNamespaces();
            
            if (namespaces != null && !namespaces.isEmpty()) {
                for (Object namespaceObj : namespaces) {
                    Map<String, Object> ns = (Map<String, Object>) namespaceObj;
                    String namespaceId = String.valueOf(ns.get("namespace"));
                    String namespaceShowName = String.valueOf(ns.getOrDefault("namespaceShowName", ns.get("namespaceShowName")));
                    
                    Map<String, String> namespaceInfo = new HashMap<>();
                    namespaceInfo.put("namespaceShowName", namespaceShowName);
                    namespaceInfo.put("namespace", namespaceId);
                    Object descObj = ns.getOrDefault("namespaceDesc", ns.get("description"));
                    namespaceInfo.put("namespaceDesc", descObj != null ? String.valueOf(descObj) : "");
                    namespaceInfo.put("configCount", String.valueOf(ns.getOrDefault("configCount", "0")));
                    
                    String[] row = new String[listFields.length];
                    for (int j = 0; j < listFields.length; j++) {
                        row[j] = namespaceInfo.getOrDefault(listFields[j], "");
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
            List<?> namespaces = nacosApi.getNamespaces();
            return namespaces != null ? namespaces.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean contains(String id) {
        try {
            NacosApi nacosApi = getNacosApi();
            List<?> namespaces = nacosApi.getNamespaces();
            if (namespaces != null) {
                for (Object namespaceObj : namespaces) {
                    Map<String, Object> namespace = (Map<String, Object>) namespaceObj;
                    if (id.equals(String.valueOf(namespace.get("namespace")))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Map<String, String> show(String id) {
        Map<String, String> result = new HashMap<>();
        result.put("namespace", id);
        
        try {
            NacosApi nacosApi = getNacosApi();
            List<?> namespaces = nacosApi.getNamespaces();
            
            if (namespaces != null && !namespaces.isEmpty()) {
                for (Object namespaceObj : namespaces) {
                    Map<String, Object> ns = (Map<String, Object>) namespaceObj;
                    String nsId = String.valueOf(ns.get("namespace"));
                    if (id.equals(nsId)) {
                        result.put("namespaceShowName", String.valueOf(ns.getOrDefault("namespaceShowName", ns.get("namespaceShowName"))));
                        Object descObj = ns.getOrDefault("namespaceDesc", ns.get("namespaceDesc"));
                        if (descObj != null && !"null".equals(String.valueOf(descObj))) {
                            result.put("namespaceDesc", String.valueOf(descObj));
                        }
                        result.put("configCount", String.valueOf(ns.getOrDefault("configCount", "0")));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        String namespaceId = data.get("namespace");
        String namespaceShowName = data.get("namespaceShowName");
        String namespaceDesc = data.get("namespaceDesc");
        
        try {
            NacosApi nacosApi = getNacosApi();
            boolean success = nacosApi.createNamespace(namespaceId, namespaceShowName, namespaceDesc);
            if (!success) {
                handleFailure(NacosConstants.ERROR_NAMESPACE_CREATE_FAILED, "返回结果不是 true");
            }
        } catch (Exception e) {
            handleFailure(NacosConstants.ERROR_NAMESPACE_CREATE_FAILED, e);
        }
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        if (isPublicNamespace(id)) {
            throw new Exception(NacosConstants.ERROR_PUBLIC_NAMESPACE_READONLY);
        }
        
        String namespaceShowName = data.get("namespaceShowName");
        String namespaceDesc = data.get("namespaceDesc");

        
        if (!contains(id)) {
            throw new Exception(NacosConstants.ERROR_NAMESPACE_NOT_EXISTS);
        }
        
        try {
            NacosApi nacosApi = getNacosApi();
            boolean success = nacosApi.updateNamespace(id, namespaceShowName.trim(), namespaceDesc != null ? namespaceDesc.trim() : null);
            if (!success) {
                handleFailure(NacosConstants.ERROR_NAMESPACE_UPDATE_FAILED, "返回结果不是 true");
            }
        } catch (Exception e) {
            handleFailure(NacosConstants.ERROR_NAMESPACE_UPDATE_FAILED, e);
        }
    }

    @Override
    public void delete(String id) throws Exception {
        if (isPublicNamespace(id)) {
            throw new Exception(NacosConstants.ERROR_PUBLIC_NAMESPACE_READONLY);
        }
        
        try {
            NacosApi nacosApi = getNacosApi();
            boolean success = nacosApi.deleteNamespace(id);
            if (!success) {
                handleFailure(NacosConstants.ERROR_NAMESPACE_DELETE_FAILED, "The returned result is not true.");
            }
        } catch (Exception e) {
            handleFailure(NacosConstants.ERROR_NAMESPACE_DELETE_FAILED, e);
        }
    }
}