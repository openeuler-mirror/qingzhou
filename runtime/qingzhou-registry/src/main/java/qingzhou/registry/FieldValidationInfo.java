package qingzhou.registry;

public class FieldValidationInfo {
    public final boolean required;
    public final long numberMin;
    public final long numberMax;
    public final int lengthMin;
    public final int lengthMax;
    public final boolean asHostname;
    public final boolean asPort;
    public final boolean asURL;
    public final String unsupportedCharacters;
    public final String[] unsupportedStrings;
    public final boolean cannotAdd;
    public final boolean cannotUpdate;
    public final String effectiveWhen;

    public FieldValidationInfo(boolean required, long numberMin, long numberMax, int lengthMin, int lengthMax, boolean asHostname, boolean asPort, boolean asURL, String unsupportedCharacters, String[] unsupportedStrings, boolean cannotAdd, boolean cannotUpdate, String effectiveWhen) {
        this.required = required;
        this.numberMin = numberMin;
        this.numberMax = numberMax;
        this.lengthMin = lengthMin;
        this.lengthMax = lengthMax;
        this.asHostname = asHostname;
        this.asPort = asPort;
        this.asURL = asURL;
        this.unsupportedCharacters = unsupportedCharacters;
        this.unsupportedStrings = unsupportedStrings;
        this.cannotAdd = cannotAdd;
        this.cannotUpdate = cannotUpdate;
        this.effectiveWhen = effectiveWhen;
    }
}
