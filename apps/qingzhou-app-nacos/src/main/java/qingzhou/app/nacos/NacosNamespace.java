package qingzhou.app.nacos;

import java.util.*;

import qingzhou.api.InputType;
import qingzhou.api.action.*;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;

@Model(code = "nacos-namespace", order = 4, icon = "FolderOpened",
        name = {"命名空间", "en:Namespace Management"}, info = {"Nacos命名空间管理", "en:Nacos Namespace Management"})
public class NacosNamespace extends NacosModelBase implements Page, Show, Add, Update, Delete {

    @ModelField(name = {"命名空间名称", "en:Namespace Name"}, list = true, required = true)
    public String namespaceShowName;
    @ModelField(name = {"命名空间ID", "en:Namespace ID"}, list = true, id = true)
    public String namespace;
    @ModelField(name = {"描述", "en:Description"}, list = true, required = true, input_type = InputType.textarea)
    public String namespaceDesc;
    @ModelField(name = {"配置数", "en:Config Count"}, list = true, update = false, add = false)
    public String configCount;

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) {
        try {
            List<?> namespaces = getNacosApi().getNamespaces();
            List<Map<String, String>> items = new ArrayList<>();
            if (namespaces != null) {
                for (Object namespaceObj : namespaces) {
                    Map<String, Object> ns = (Map<String, Object>) namespaceObj;
                    Map<String, String> info = new HashMap<>();
                    info.put("namespace", String.valueOf(ns.get("namespace")));
                    info.put("namespaceShowName", String.valueOf(ns.get("namespaceShowName")));
                    // 兼容旧版本 Nacos 返回的 description 字段
                    Object desc = ns.getOrDefault("namespaceDesc", ns.get("description"));
                    info.put("namespaceDesc", desc != null ? String.valueOf(desc) : "");
                    info.put("configCount", String.valueOf(ns.getOrDefault("configCount", "0")));
                    items.add(info);
                }
            }
            return toRows(items, listFields);
        } catch (Exception e) {
            logError("Nacos namespace query failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public int total(Map<String, String> query) {
        try {
            List<?> namespaces = getNacosApi().getNamespaces();
            return namespaces != null ? namespaces.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean contains(String id) {
        try {
            List<?> namespaces = getNacosApi().getNamespaces();
            if (namespaces != null) {
                for (Object namespaceObj : namespaces) {
                    Map<String, Object> ns = (Map<String, Object>) namespaceObj;
                    if (id.equals(String.valueOf(ns.get("namespace")))) return true;
                }
            }
        } catch (Exception e) {
            logError("Nacos namespace query failed", e);
        }
        return false;
    }

    @Override
    public Map<String, String> show(String id) {
        Map<String, String> result = new HashMap<>();
        result.put("namespace", id);
        try {
            List<?> namespaces = getNacosApi().getNamespaces();
            if (namespaces != null) {
                for (Object namespaceObj : namespaces) {
                    Map<String, Object> ns = (Map<String, Object>) namespaceObj;
                    if (!id.equals(String.valueOf(ns.get("namespace")))) continue;
                    result.put("namespaceShowName", String.valueOf(ns.get("namespaceShowName")));
                    Object desc = ns.get("namespaceDesc");
                    if (desc != null && !"null".equals(String.valueOf(desc))) {
                        result.put("namespaceDesc", String.valueOf(desc));
                    }
                    result.put("configCount", String.valueOf(ns.getOrDefault("configCount", "0")));
                    break;
                }
            }
        } catch (Exception e) {
            logError("Nacos namespace query failed", e);
        }
        return result;
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        String namespaceId = data.get("namespace");
        String name = data.get("namespaceShowName");
        String desc = data.get("namespaceDesc");
        writeOrFail(NacosConstants.ERROR_NAMESPACE_CREATE_FAILED, "返回结果不是 true",
                () -> getNacosApi().createNamespace(namespaceId, name, desc));
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        if (isPublicNamespace(id)) throw new Exception(NacosConstants.ERROR_PUBLIC_NAMESPACE_READONLY);
        if (!contains(id)) throw new Exception(NacosConstants.ERROR_NAMESPACE_NOT_EXISTS);
        String name = data.get("namespaceShowName");
        String desc = data.get("namespaceDesc");
        writeOrFail(NacosConstants.ERROR_NAMESPACE_UPDATE_FAILED, "返回结果不是 true",
                () -> getNacosApi().updateNamespace(id, name.trim(), desc != null ? desc.trim() : null));
    }

    @Override
    public void delete(String id) throws Exception {
        if (isPublicNamespace(id)) throw new Exception(NacosConstants.ERROR_PUBLIC_NAMESPACE_READONLY);
        writeOrFail(NacosConstants.ERROR_NAMESPACE_DELETE_FAILED, "The returned result is not true.",
                () -> getNacosApi().deleteNamespace(id));
    }
}
