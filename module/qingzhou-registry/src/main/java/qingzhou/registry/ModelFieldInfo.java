package qingzhou.registry;

import qingzhou.api.FieldType;
import qingzhou.api.InputType;

public class ModelFieldInfo {
    private String code;
    private String[] name;
    private String[] info;
    private String group;
    private FieldType fieldType;
    private InputType inputType;
    private String refModel;
    private String separator;
    private transient Class<?> refModelClass;
    private String defaultValue;
    private boolean show;
    private boolean create;
    private boolean edit;
    private boolean update;
    private boolean list;
    private int ignore;
    private boolean search;
    private boolean linkShow;
    private int widthPercent;
    private boolean numeric;
    private String display;
    private boolean required;
    private long min;
    private long max;
    private int lengthMin;
    private int lengthMax;
    private boolean host;
    private boolean port;
    private String[] forbid;
    private String[] skip;
    private String pattern;
    private boolean readonly;
    private String linkList;
    private String[] color;
    private String[] echoGroup;

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

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
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

    public boolean isLinkShow() {
        return linkShow;
    }

    public void setLinkShow(boolean linkShow) {
        this.linkShow = linkShow;
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

    public Class<?> getRefModelClass() {
        return refModelClass;
    }

    public void setRefModelClass(Class<?> refModelClass) {
        this.refModelClass = refModelClass;
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

    public String[] getSkip() {
        return skip;
    }

    public void setSkip(String[] skip) {
        this.skip = skip;
    }

    public String[] getForbid() {
        return forbid;
    }

    public void setForbid(String[] forbid) {
        this.forbid = forbid;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public boolean isEmail() {
        return email;
    }

    public void setEmail(boolean email) {
        this.email = email;
    }

    public String getLinkList() {
        return linkList;
    }

    public void setLinkList(String linkList) {
        this.linkList = linkList;
    }

    public String[] getEchoGroup() {
        return echoGroup;
    }

    public void setEchoGroup(String[] echoGroup) {
        this.echoGroup = echoGroup;
    }
}
