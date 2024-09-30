package qingzhou.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import qingzhou.api.FieldType;
import qingzhou.engine.util.Utils;

public class ModelInfo {
    private String code;
    private String[] name;
    private String[] info;
    private String icon;
    private String menu;
    private int order;
    private String entrance;
    private boolean hidden;
    private String idField;

    private ModelFieldInfo[] modelFieldInfos;
    private ModelActionInfo[] modelActionInfos;
    private GroupInfo[] groupInfos;

    public ModelActionInfo getModelActionInfo(String actionName) {
        return Arrays.stream(modelActionInfos).filter(modelActionInfo -> modelActionInfo.getCode().equals(actionName)).findAny().orElse(null);
    }

    public String[] getListActionNames() {
        return Arrays.stream(modelActionInfos)
                .filter(modelActionInfo -> modelActionInfo.getOrder() > 0)
                .sorted(Comparator.comparingInt(ModelActionInfo::getOrder))
                .map(ModelActionInfo::getCode)
                .toArray(String[]::new);
    }

    public String[] getHeadActionNames() {
        return Arrays.stream(modelActionInfos)
                .filter(ModelActionInfo::isHead)
                .sorted(Comparator.comparingInt(ModelActionInfo::getOrder))
                .map(ModelActionInfo::getCode)
                .toArray(String[]::new);
    }

    public String[] getBatchActionNames() {
        return Arrays.stream(modelActionInfos)
                .filter(modelActionInfo -> modelActionInfo.getOrder() > 0)
                .filter(ModelActionInfo::isBatch)
                .sorted(Comparator.comparingInt(ModelActionInfo::getOrder))
                .map(ModelActionInfo::getCode)
                .toArray(String[]::new);
    }

    public String[] getActionNames() {
        return Arrays.stream(modelActionInfos).map(ModelActionInfo::getCode).toArray(String[]::new);
    }

    public Map<String, String> getFormFieldDefaultValues() {
        return Arrays.stream(modelFieldInfos).filter(modelFieldInfo -> !modelFieldInfo.isMonitor()).collect(Collectors.toMap(ModelFieldInfo::getCode, ModelFieldInfo::getDefaultValue));
    }

    public ModelFieldInfo getModelFieldInfo(String fieldName) {
        for (ModelFieldInfo fieldInfo : modelFieldInfos) {
            if (fieldInfo.getCode().equals(fieldName)) {
                return fieldInfo;
            }
        }
        return null;
    }

    public Integer[] getFieldsIndexToList() {
        List<Integer> index = new ArrayList<>();
        int idFieldIndex = -1;
        for (int i = 0; i < modelFieldInfos.length; i++) {
            ModelFieldInfo fieldInfo = modelFieldInfos[i];

            if (fieldInfo.getCode().equals(idField)) {
                idFieldIndex = i;
                continue;
            }
            if (fieldInfo.isMonitor()) continue;

            if (fieldInfo.isList()) index.add(i);
        }
        if (idFieldIndex >= 0) {
            index.add(0, idFieldIndex);
        }
        return index.toArray(new Integer[0]);
    }

    public String[] getFieldsToList() {
        List<String> list = new ArrayList<>();
        for (Integer i : getFieldsIndexToList()) {
            list.add(modelFieldInfos[i].getCode());
        }
        return list.toArray(new String[0]);
    }

    public String[] getMonitorFieldNames() {
        return Arrays.stream(modelFieldInfos).filter(ModelFieldInfo::isMonitor).map(ModelFieldInfo::getCode).toArray(String[]::new);
    }

    public String[] getFileUploadFieldNames() {
        return Arrays.stream(modelFieldInfos).filter(modelFieldInfo -> !modelFieldInfo.isMonitor() && modelFieldInfo.getType().equals(FieldType.file.name())).map(ModelFieldInfo::getCode).toArray(String[]::new);
    }

    public String[] getFormFieldNames() {
        return Arrays.stream(modelFieldInfos).filter(modelFieldInfo -> !modelFieldInfo.isMonitor()).map(ModelFieldInfo::getCode).toArray(String[]::new);
    }

    public Map<String, String> getShowMap() {
        Map<String, String> data = new HashMap<>();
        for (String field : getFormFieldNames()) {
            ModelFieldInfo modelFieldInfo = getModelFieldInfo(field);
            String show = modelFieldInfo.getShow();
            if (Utils.notBlank(show)) {
                data.put(field, show);
            }
        }
        return data;
    }

    public Map<String, String> getReadOnlyMap() {
        Map<String, String> data = new HashMap<>();
        for (String field : getFormFieldNames()) {
            ModelFieldInfo modelFieldInfo = getModelFieldInfo(field);
            String readOnly = modelFieldInfo.getReadOnly();
            if (Utils.notBlank(readOnly)) {
                data.put(field, readOnly);
            }
        }
        return data;
    }

    public Map<String, Map<String, ModelFieldInfo>> getFormGroupedFields() {
        return getGroupedFields(getFormFieldNames());
    }

    private Map<String, Map<String, ModelFieldInfo>> getGroupedFields(String[] fieldNames) {
        Map<String, Map<String, ModelFieldInfo>> result = new LinkedHashMap<>();
        Map<String, ModelFieldInfo> defaultGroup = new LinkedHashMap<>();
        for (String formField : fieldNames) {
            ModelFieldInfo fieldInfo = getModelFieldInfo(formField);
            String group = fieldInfo.getGroup();
            if (Utils.isBlank(group)) {
                defaultGroup.put(formField, fieldInfo);
            } else {
                result.computeIfAbsent(group, k -> new LinkedHashMap<>()).put(formField, fieldInfo);
            }
        }

        if (!defaultGroup.isEmpty()) {
            result.put("", defaultGroup);
        }

        return result;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String[] getName() {
        return name;
    }

    public void setName(String[] name) {
        this.name = name;
    }

    public String[] getInfo() {
        return info;
    }

    public void setInfo(String[] info) {
        this.info = info;
    }

    public String getEntrance() {
        return entrance;
    }

    public void setEntrance(String entrance) {
        this.entrance = entrance;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getIdField() {
        return idField;
    }

    public void setIdField(String idField) {
        this.idField = idField;
    }

    public String getMenu() {
        return menu;
    }

    public void setMenu(String menu) {
        this.menu = menu;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public GroupInfo[] getGroupInfos() {
        return groupInfos;
    }

    public void setGroupInfos(GroupInfo[] groupInfos) {
        this.groupInfos = groupInfos;
    }

    public ModelFieldInfo[] getModelFieldInfos() {
        return modelFieldInfos;
    }

    public void setModelFieldInfos(ModelFieldInfo[] modelFieldInfos) {
        this.modelFieldInfos = modelFieldInfos;
    }

    public ModelActionInfo[] getModelActionInfos() {
        return modelActionInfos;
    }

    public void setModelActionInfos(ModelActionInfo[] modelActionInfos) {
        this.modelActionInfos = modelActionInfos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelInfo modelInfo = (ModelInfo) o;
        return Objects.equals(code, modelInfo.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
