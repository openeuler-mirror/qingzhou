package qingzhou.registry;

public class ModelFieldInfo {
    private String code;
    private String[] name;
    private String[] info;
    private String group;
    private String type;
    private String[] options;
    private String refModel;
    private String defaultValue;
    private boolean list;
    private boolean monitor;
    private boolean numeric;
    private boolean required;
    private long min;
    private long max;
    private int lengthMin;
    private int lengthMax;
    private boolean port;
    private String unsupportedCharacters;
    private String[] unsupportedStrings;

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

    public boolean isList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public String getRefModel() {
        return refModel;
    }

    public void setRefModel(String refModel) {
        this.refModel = refModel;
    }

    public boolean isMonitor() {
        return monitor;
    }

    public void setMonitor(boolean monitor) {
        this.monitor = monitor;
    }

    public boolean isNumeric() {
        return numeric;
    }

    public void setNumeric(boolean numeric) {
        this.numeric = numeric;
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

    public boolean isPort() {
        return port;
    }

    public void setPort(boolean port) {
        this.port = port;
    }

    public String getUnsupportedCharacters() {
        return unsupportedCharacters;
    }

    public void setUnsupportedCharacters(String unsupportedCharacters) {
        this.unsupportedCharacters = unsupportedCharacters;
    }

    public String[] getUnsupportedStrings() {
        return unsupportedStrings;
    }

    public void setUnsupportedStrings(String[] unsupportedStrings) {
        this.unsupportedStrings = unsupportedStrings;
    }
}
