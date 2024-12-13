package qingzhou.core.registry;

import qingzhou.api.ActionType;
import qingzhou.api.FieldType;
import qingzhou.api.InputType;

import java.io.Serializable;

public class ModelFieldInfo implements Serializable {
    private String code;
    private String[] name;
    private String[] info;
    private String group;
    private FieldType fieldType;
    private InputType inputType;
    private String refModel;
    private String separator;

    private String defaultValue;
    private boolean show;
    private boolean hidden;
    private boolean create;
    private boolean edit;
    private String updateAction;
    private boolean list;
    private int ignore;
    private boolean search;
    private String linkAction;
    private int widthPercent;
    private boolean numeric;
    private String display;
    private boolean required;
    private boolean id;
    private boolean idMask;
    private long min;
    private long max;
    private int lengthMin;
    private int lengthMax;
    private boolean host;
    private boolean port;
    private String[] forbid;
    private String[] xssSkip;
    private String pattern;
    private boolean plainText;

    private boolean searchMultiple;
    private boolean readonly;
    private String linkModel;
    private transient Class<?> refModelClass;
    private ActionType actionType;
    private String[] color;
    private String[] echoGroup;
    private boolean skipValidate;
    private boolean staticOption;
    private boolean dynamicOption;
    private boolean showLabel;
    private boolean sameLine;
    private String placeholder;
    private String[] combineFields;
    private String order;

    public String[] getCombineFields() {
        return combineFields;
    }

    public void setCombineFields(String[] combineFields) {
        this.combineFields = combineFields;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isSearchMultiple() {
        return searchMultiple;
    }

    public void setSearchMultiple(boolean searchMultiple) {
        this.searchMultiple = searchMultiple;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public String[] getColor() {
        return color;
    }

    public void setColor(String[] color) {
        this.color = color;
    }

    public boolean isFile() {
        return file;
    }

    public void setFile(boolean file) {
        this.file = file;
    }

    private boolean email;
    private boolean file;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public String getUpdateAction() {
        return updateAction;
    }

    public void setUpdateAction(String updateAction) {
        this.updateAction = updateAction;
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isEdit() {
        return edit;
    }

    public void setEdit(boolean edit) {
        this.edit = edit;
    }

    public boolean isList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
    }

    public int getIgnore() {
        return ignore;
    }

    public void setIgnore(int ignore) {
        this.ignore = ignore;
    }

    public boolean isSearch() {
        return search;
    }

    public void setSearch(boolean search) {
        this.search = search;
    }

    public String getLinkAction() {
        return linkAction;
    }

    public void setLinkAction(String linkAction) {
        this.linkAction = linkAction;
    }

    public int getWidthPercent() {
        return widthPercent;
    }

    public void setWidthPercent(int widthPercent) {
        this.widthPercent = widthPercent;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public String getRefModel() {
        return refModel;
    }

    public void setRefModel(String refModel) {
        this.refModel = refModel;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public boolean isNumeric() {
        return numeric;
    }

    public void setNumeric(boolean numeric) {
        this.numeric = numeric;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isId() {
        return id;
    }

    public void setId(boolean id) {
        this.id = id;
    }

    public boolean isIdMask() {
        return idMask;
    }

    public void setIdMask(boolean idMask) {
        this.idMask = idMask;
    }

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public int getLengthMin() {
        return lengthMin;
    }

    public void setLengthMin(int lengthMin) {
        this.lengthMin = lengthMin;
    }

    public int getLengthMax() {
        return lengthMax;
    }

    public void setLengthMax(int lengthMax) {
        this.lengthMax = lengthMax;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public boolean isPort() {
        return port;
    }

    public void setPort(boolean port) {
        this.port = port;
    }

    public String[] getXssSkip() {
        return xssSkip;
    }

    public void setXssSkip(String[] xssSkip) {
        this.xssSkip = xssSkip;
    }

    public String[] getForbid() {
        return forbid;
    }

    public void setForbid(String[] forbid) {
        this.forbid = forbid;
    }

    public boolean isPlainText() {
        return plainText;
    }

    public void setPlainText(boolean plainText) {
        this.plainText = plainText;
    }

    public boolean isEmail() {
        return email;
    }

    public void setEmail(boolean email) {
        this.email = email;
    }

    public String getLinkModel() {
        return linkModel;
    }

    public void setLinkModel(String linkModel) {
        this.linkModel = linkModel;
    }

    public Class<?> getRefModelClass() {
        return refModelClass;
    }

    public void setRefModelClass(Class<?> refModelClass) {
        this.refModelClass = refModelClass;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String[] getEchoGroup() {
        return echoGroup;
    }

    public void setEchoGroup(String[] echoGroup) {
        this.echoGroup = echoGroup;
    }

    public boolean isSkipValidate() {
        return skipValidate;
    }

    public void setSkipValidate(boolean skipValidate) {
        this.skipValidate = skipValidate;
    }

    public boolean isStaticOption() {
        return staticOption;
    }

    public void setStaticOption(boolean staticOption) {
        this.staticOption = staticOption;
    }

    public boolean isDynamicOption() {
        return dynamicOption;
    }

    public void setDynamicOption(boolean dynamicOption) {
        this.dynamicOption = dynamicOption;
    }

    public boolean isSameLine() {
        return sameLine;
    }

    public void setSameLine(boolean sameLine) {
        this.sameLine = sameLine;
    }

    public boolean isShowLabel() {
        return showLabel;
    }

    public void setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
    }
}
