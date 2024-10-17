package qingzhou.registry;

import qingzhou.api.FieldType;
import qingzhou.engine.util.Utils;

import java.util.*;
import java.util.stream.Collectors;

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
    private boolean validate;
    private boolean showOrderNumber;

    private ModelFieldInfo[] modelFieldInfos;
    private ModelActionInfo[] modelActionInfos;
    private ItemInfo[] groupInfos;
    private LinkedHashMap<String, ItemInfo[]> optionInfos;
    private Map<String, String> filterValues;
    private boolean showIdField;
    private String[] staticOptionFields;
    private String[] dynamicOptionFields;
    private String[] headActions;
    private String[] listActions;
    private String[] batchActions;

    public ModelActionInfo getModelActionInfo(String actionName) {
        return Arrays.stream(modelActionInfos).filter(modelActionInfo -> modelActionInfo.getCode().equals(actionName)).findAny().orElse(null);
    }

    public String[] getHeadActions() {
        return existsActions(headActions);
    }

    public String[] getListActions() {
        return existsActions(listActions);
    }

    public String[] getBatchActions() {
        return existsActions(batchActions);
    }

    public void setBatchActions(String[] batchActions) {
        this.batchActions = batchActions;
    }

    private String[] existsActions(String[] scope) {
        if (scope == null) return new String[0];

        List<String> fountActions = new ArrayList<>();
        for (String action : scope) {
            if (getModelActionInfo(action) != null) {
                fountActions.add(action);
            }
        }
        return fountActions.toArray(new String[0]);
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

    public String[] getFieldsToListSearch() {
        List<String> list = new ArrayList<>();
        for (ModelFieldInfo fieldInfo : modelFieldInfos) {
            if (fieldInfo.isSearch()) {
                list.add(fieldInfo.getCode());
            }
        }
        return list.toArray(new String[0]);
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

    public ItemInfo[] getGroupInfos() {
        return groupInfos;
    }

    public void setGroupInfos(ItemInfo[] groupInfos) {
        this.groupInfos = groupInfos;
    }

    public LinkedHashMap<String, ItemInfo[]> getOptionInfos() {
        return optionInfos;
    }

    public void setOptionInfos(LinkedHashMap<String, ItemInfo[]> optionInfos) {
        this.optionInfos = optionInfos;
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

    public boolean isValidate() {
        return validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public boolean isShowOrderNumber() {
        return showOrderNumber;
    }

    public void setShowOrderNumber(boolean showOrderNumber) {
        this.showOrderNumber = showOrderNumber;
    }

    public Map<String, String> getFilterValues() {
        return filterValues;
    }

    public void setFilterValues(Map<String, String> filterValues) {
        this.filterValues = filterValues;
    }

    public boolean isShowIdField() {
        return showIdField;
    }

    public void setShowIdField(boolean showIdField) {
        this.showIdField = showIdField;
    }

    public String[] getStaticOptionFields() {
        return staticOptionFields;
    }

    public void setStaticOptionFields(String[] staticOptionFields) {
        this.staticOptionFields = staticOptionFields;
    }

    public String[] getDynamicOptionFields() {
        return dynamicOptionFields;
    }

    public void setDynamicOptionFields(String[] dynamicOptionFields) {
        this.dynamicOptionFields = dynamicOptionFields;
    }

    public void setListActions(String[] listActions) {
        this.listActions = listActions;
    }

    public void setHeadActions(String[] headActions) {
        this.headActions = headActions;
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
