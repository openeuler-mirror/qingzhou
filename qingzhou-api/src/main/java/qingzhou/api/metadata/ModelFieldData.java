package qingzhou.api.metadata;

import qingzhou.api.FieldType;

public interface ModelFieldData {
    String group();

    String[] nameI18n();

    String[] infoI18n();

    boolean required();

    FieldType type();

    String refModel();

    long min();

    long max();

    int minLength();

    int maxLength();

    boolean isIpOrHostname();

    boolean isWildcardIp();

    boolean isPort();

    boolean isPattern();

    boolean isURL();

    String noGreaterThanMinusOne();

    String noGreaterThan();

    String noGreaterOrEqualThanDate();

    String noLessThan();

    String noLessOrEqualThanDate();

    boolean noLessThanCurrentTime();

    String notSupportedCharacters();

    String[] notSupportedStrings();

    boolean noSupportZHChar();

    String cannotBeTheSameAs();

    boolean skipSafeCheck();

    String skipCharacterCheck();

    boolean checkXssLevel1();

    boolean clientEncrypt();

    String effectiveWhen();

    boolean disableOnCreate();

    boolean disableOnEdit();

    boolean showToEdit();

    boolean showToList();

    String linkModel();

    String valueFrom();

    boolean isMonitorField();

    boolean supportGraphical();

    boolean supportGraphicalDynamic();
}
