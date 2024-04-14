package qingzhou.registry;

public class FieldValidationInfo {
    public boolean required;
    public long numberMin;
    public long numberMax;
    public int lengthMin;
    public int lengthMax;
    public boolean hostname;
    public boolean port;
    public String unsupportedCharacters;
    public String[] unsupportedStrings;
    public boolean cannotAdd;
    public boolean cannotUpdate;
    public String effectiveWhen;

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public long getNumberMin() {
        return numberMin;
    }

    public void setNumberMin(long numberMin) {
        this.numberMin = numberMin;
    }

    public long getNumberMax() {
        return numberMax;
    }

    public void setNumberMax(long numberMax) {
        this.numberMax = numberMax;
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

    public boolean isHostname() {
        return hostname;
    }

    public void setHostname(boolean hostname) {
        this.hostname = hostname;
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

    public boolean isCannotAdd() {
        return cannotAdd;
    }

    public void setCannotAdd(boolean cannotAdd) {
        this.cannotAdd = cannotAdd;
    }

    public boolean isCannotUpdate() {
        return cannotUpdate;
    }

    public void setCannotUpdate(boolean cannotUpdate) {
        this.cannotUpdate = cannotUpdate;
    }

    public String getEffectiveWhen() {
        return effectiveWhen;
    }

    public void setEffectiveWhen(String effectiveWhen) {
        this.effectiveWhen = effectiveWhen;
    }
}
