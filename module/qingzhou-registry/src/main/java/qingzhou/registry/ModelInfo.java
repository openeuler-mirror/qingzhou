package qingzhou.registry;

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
    private String idFieldName;

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
        for (int i = 0; i < modelFieldInfos.length; i++) {
            ModelFieldInfo fieldInfo = modelFieldInfos[i];
            if (fieldInfo.isMonitor()) continue;
            if (fieldInfo.isList()) {
                index.add(i);
            }
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

    public String[] getFormFieldNames() {
        return Arrays.stream(modelFieldInfos).filter(modelFieldInfo -> !modelFieldInfo.isMonitor()).map(ModelFieldInfo::getCode).toArray(String[]::new);
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

    public String getIdFieldName() {
        return idFieldName;
    }

    public void setIdFieldName(String idFieldName) {
        this.idFieldName = idFieldName;
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
}
