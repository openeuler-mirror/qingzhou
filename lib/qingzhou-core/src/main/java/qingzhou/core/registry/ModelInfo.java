package qingzhou.core.registry;

import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import qingzhou.api.FieldType;
import qingzhou.api.InputType;
import qingzhou.engine.util.Utils;

public class ModelInfo implements Serializable {
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
    private Map<String, String> defaultSearch;
    private boolean useDynamicDefaultSearch;
    private String[] staticOptionFields;
    private String[] dynamicOptionFields;
    private String[] headActions;
    private String[] listActions;
    private String[] batchActions;
    private String[] formActions;

    public Map<String, List<String>> getSameLineMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        List<String> formFieldNames = new LinkedList<>(Arrays.asList(getFormFieldNames()));
        String firstFieldName = null;
        for (String formFieldName : formFieldNames) {
            ModelFieldInfo modelFieldInfo = getModelFieldInfo(formFieldName);
            if (firstFieldName != null && modelFieldInfo.isSameLine()) {
                map.computeIfAbsent(firstFieldName, key -> new ArrayList<>()).add(formFieldName);
            } else {
                firstFieldName = formFieldName;
            }
        }
        return map;
    }

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

    public String[] getFormActions() {
        return existsActions(formActions);
    }

    public void setBatchActions(String[] batchActions) {
        this.batchActions = batchActions;
    }

    public void setFormActions(String[] formActions) {
        this.formActions = formActions;
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
        return Arrays.stream(modelFieldInfos).filter(modelFieldInfo -> modelFieldInfo.getFieldType() == FieldType.FORM)
                .collect(Collectors.toMap(ModelFieldInfo::getCode, ModelFieldInfo::getDefaultValue, (s, s2) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", s));
                }, (Supplier<Map<String, String>>) LinkedHashMap::new)); // 保证 form 表单的 顺序
    }

    public ModelFieldInfo getModelFieldInfo(String fieldName) {
        for (ModelFieldInfo fieldInfo : modelFieldInfos) {
            if (fieldInfo.getCode().equals(fieldName)) {
                return fieldInfo;
            }
        }
        return null;
    }

    public String[] getFieldsToListSearch() {
        List<String> list = new LinkedList<>();
        for (ModelFieldInfo fieldInfo : modelFieldInfos) {
            if (fieldInfo.isSearch()) {
                list.add(fieldInfo.getCode());
            }
        }
        return list.toArray(new String[0]);
    }

    public String[] getFieldsToList() {
        List<String> list = new LinkedList<>();
        for (String formFieldName : getFormFieldNames()) {
            ModelFieldInfo modelFieldInfo = getModelFieldInfo(formFieldName);
            if (modelFieldInfo.isList() && modelFieldInfo.isShow()) {
                list.add(formFieldName);
            }
        }
        if (!list.contains(idField)) {
            list.add(0, idField);
        }
        return list.toArray(new String[0]);
    }

    public String[] getAllFieldNames() {
        return Arrays.stream(modelFieldInfos).map(ModelFieldInfo::getCode).toArray(String[]::new);
    }

    public String[] getMonitorFieldNames() {
        return Arrays.stream(modelFieldInfos).filter(modelFieldInfo -> modelFieldInfo.getFieldType() == FieldType.MONITOR).map(ModelFieldInfo::getCode).toArray(String[]::new);
    }

    public String[] getFileUploadFieldNames() {
        return Arrays.stream(modelFieldInfos).filter(modelFieldInfo -> modelFieldInfo.getFieldType() == FieldType.FORM && modelFieldInfo.getInputType() == InputType.file).map(ModelFieldInfo::getCode).toArray(String[]::new);
    }

    public String[] getFormFieldNames() {
        return Arrays.stream(modelFieldInfos).filter(modelFieldInfo -> modelFieldInfo.getFieldType() == FieldType.FORM).map(ModelFieldInfo::getCode).toArray(String[]::new);
    }

    public Map<String, String> getFormFieldDisplay() {
        Map<String, String> data = new HashMap<>();
        for (String field : getFormFieldNames()) {
            ModelFieldInfo modelFieldInfo = getModelFieldInfo(field);
            String show = modelFieldInfo.getDisplay();
            if (Utils.notBlank(show)) {
                data.put(field, show);
            }
        }
        return data;
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
        Map<Integer, Integer> temp = new LinkedHashMap<>();
        for (int i = 0; i < modelFieldInfos.length; i++) {
            int index = modelFieldInfos[i].getIndex();
            if (index >= 0 && index < modelFieldInfos.length) {
                temp.put(i, index);
            }
        }
        for (Map.Entry<Integer, Integer> e : temp.entrySet()) {
            Utils.swap(modelFieldInfos, e.getKey(), e.getValue());
        }

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

    public Map<String, String> getDefaultSearch() {
        return defaultSearch;
    }

    public void setDefaultSearch(Map<String, String> defaultSearch) {
        if (defaultSearch == null || defaultSearch.isEmpty()) return;
        this.defaultSearch = new HashMap<>(defaultSearch);
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

    public boolean isUseDynamicDefaultSearch() {
        return useDynamicDefaultSearch;
    }

    public void setUseDynamicDefaultSearch(boolean useDynamicDefaultSearch) {
        this.useDynamicDefaultSearch = useDynamicDefaultSearch;
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
