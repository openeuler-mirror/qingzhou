package qingzhou.serialization;

import qingzhou.api.FieldType;

import java.io.Serializable;

public class ModelFieldData implements Serializable {
    private String group = "";
    private String[] nameI18n;
    private String[] infoI18n;
    private boolean required = false;
    private FieldType type = FieldType.text;
    private String refModel = "";
    private long min = -1;
    private long max = 1000000000L;
    private int minLength = 0;
    private int maxLength = 1000;
    private boolean isIpOrHostname = false;
    private boolean isWildcardIp = false;
    private boolean isPort = false;
    private boolean isPattern = false;
    private boolean isURL = false;
    private String noGreaterThanMinusOne = "";
    private String noGreaterThan = "";
    private String noGreaterOrEqualThanDate = "";
    private String noLessThan = "";
    private String noLessOrEqualThanDate = "";
    private boolean noLessThanCurrentTime = false;
    private String notSupportedCharacters = "";
    private String[] notSupportedStrings = {};
    private boolean noSupportZHChar = false;
    private String cannotBeTheSameAs = "";
    private boolean skipSafeCheck = false;
    private String skipCharacterCheck = "";
    private boolean checkXssLevel1 = false;
    private boolean clientEncrypt = false;
    private String effectiveWhen = "";
    private boolean disableOnCreate = false;
    private boolean disableOnEdit = false;
    private boolean showToEdit = true;
    private boolean showToList = false;
    private String linkModel = "";
    private String valueFrom = "";
    private boolean isMonitorField = false;
    private boolean supportGraphical = false;
    private boolean supportGraphicalDynamic = false;

    public String group() {
        return group;
    }

    public String[] nameI18n() {
        return nameI18n;
    }

    public String[] infoI18n() {
        return infoI18n;
    }

    public boolean required() {
        return required;
    }

    public FieldType type() {
        return type;
    }

    public String refModel() {
        return refModel;
    }

    public long min() {
        return min;
    }

    public long max() {
        return max;
    }

    public int minLength() {
        return minLength;
    }

    public int maxLength() {
        return maxLength;
    }

    public boolean isIpOrHostname() {
        return isIpOrHostname;
    }

    public boolean isWildcardIp() {
        return isWildcardIp;
    }

    public boolean isPort() {
        return isPort;
    }

    public boolean isPattern() {
        return isPattern;
    }

    public boolean isURL() {
        return isURL;
    }

    public String noGreaterThanMinusOne() {
        return noGreaterThanMinusOne;
    }

    public String noGreaterThan() {
        return noGreaterThan;
    }

    public String noGreaterOrEqualThanDate() {
        return noGreaterOrEqualThanDate;
    }

    public String noLessThan() {
        return noLessThan;
    }

    public String noLessOrEqualThanDate() {
        return noLessOrEqualThanDate;
    }

    public boolean noLessThanCurrentTime() {
        return noLessThanCurrentTime;
    }

    public String notSupportedCharacters() {
        return notSupportedCharacters;
    }

    public String[] notSupportedStrings() {
        return notSupportedStrings;
    }

    public boolean noSupportZHChar() {
        return noSupportZHChar;
    }

    public String cannotBeTheSameAs() {
        return cannotBeTheSameAs;
    }

    public boolean skipSafeCheck() {
        return skipSafeCheck;
    }

    public String skipCharacterCheck() {
        return skipCharacterCheck;
    }

    public boolean checkXssLevel1() {
        return checkXssLevel1;
    }

    public boolean clientEncrypt() {
        return clientEncrypt;
    }

    public String effectiveWhen() {
        return effectiveWhen;
    }

    public boolean disableOnCreate() {
        return disableOnCreate;
    }

    public boolean disableOnEdit() {
        return disableOnEdit;
    }

    public boolean showToEdit() {
        return showToEdit;
    }

    public boolean showToList() {
        return showToList;
    }

    public String linkModel() {
        return linkModel;
    }

    public String valueFrom() {
        return valueFrom;
    }

    public boolean isMonitorField() {
        return isMonitorField;
    }

    public boolean supportGraphical() {
        return supportGraphical;
    }

    public boolean supportGraphicalDynamic() {
        return supportGraphicalDynamic;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setNameI18n(String[] nameI18n) {
        this.nameI18n = nameI18n;
    }

    public void setInfoI18n(String[] infoI18n) {
        this.infoI18n = infoI18n;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public void setRefModel(String refModel) {
        this.refModel = refModel;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public void setIpOrHostname(boolean ipOrHostname) {
        isIpOrHostname = ipOrHostname;
    }

    public void setWildcardIp(boolean wildcardIp) {
        isWildcardIp = wildcardIp;
    }

    public void setPort(boolean port) {
        isPort = port;
    }

    public void setPattern(boolean pattern) {
        isPattern = pattern;
    }

    public void setURL(boolean URL) {
        isURL = URL;
    }

    public void setNoGreaterThanMinusOne(String noGreaterThanMinusOne) {
        this.noGreaterThanMinusOne = noGreaterThanMinusOne;
    }

    public void setNoGreaterThan(String noGreaterThan) {
        this.noGreaterThan = noGreaterThan;
    }

    public void setNoGreaterOrEqualThanDate(String noGreaterOrEqualThanDate) {
        this.noGreaterOrEqualThanDate = noGreaterOrEqualThanDate;
    }

    public void setNoLessThan(String noLessThan) {
        this.noLessThan = noLessThan;
    }

    public void setNoLessOrEqualThanDate(String noLessOrEqualThanDate) {
        this.noLessOrEqualThanDate = noLessOrEqualThanDate;
    }

    public void setNoLessThanCurrentTime(boolean noLessThanCurrentTime) {
        this.noLessThanCurrentTime = noLessThanCurrentTime;
    }

    public void setNotSupportedCharacters(String notSupportedCharacters) {
        this.notSupportedCharacters = notSupportedCharacters;
    }

    public void setNotSupportedStrings(String[] notSupportedStrings) {
        this.notSupportedStrings = notSupportedStrings;
    }

    public void setNoSupportZHChar(boolean noSupportZHChar) {
        this.noSupportZHChar = noSupportZHChar;
    }

    public void setCannotBeTheSameAs(String cannotBeTheSameAs) {
        this.cannotBeTheSameAs = cannotBeTheSameAs;
    }

    public void setSkipSafeCheck(boolean skipSafeCheck) {
        this.skipSafeCheck = skipSafeCheck;
    }

    public void setSkipCharacterCheck(String skipCharacterCheck) {
        this.skipCharacterCheck = skipCharacterCheck;
    }

    public void setCheckXssLevel1(boolean checkXssLevel1) {
        this.checkXssLevel1 = checkXssLevel1;
    }

    public void setClientEncrypt(boolean clientEncrypt) {
        this.clientEncrypt = clientEncrypt;
    }

    public void setEffectiveWhen(String effectiveWhen) {
        this.effectiveWhen = effectiveWhen;
    }

    public void setDisableOnCreate(boolean disableOnCreate) {
        this.disableOnCreate = disableOnCreate;
    }

    public void setDisableOnEdit(boolean disableOnEdit) {
        this.disableOnEdit = disableOnEdit;
    }

    public void setShowToEdit(boolean showToEdit) {
        this.showToEdit = showToEdit;
    }

    public void setShowToList(boolean showToList) {
        this.showToList = showToList;
    }

    public void setLinkModel(String linkModel) {
        this.linkModel = linkModel;
    }

    public void setValueFrom(String valueFrom) {
        this.valueFrom = valueFrom;
    }

    public void setMonitorField(boolean monitorField) {
        isMonitorField = monitorField;
    }

    public void setSupportGraphical(boolean supportGraphical) {
        this.supportGraphical = supportGraphical;
    }

    public void setSupportGraphicalDynamic(boolean supportGraphicalDynamic) {
        this.supportGraphicalDynamic = supportGraphicalDynamic;
    }

}