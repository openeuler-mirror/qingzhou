package qingzhou.api;

public interface Option {
    String value();

    String[] i18n();

    static Option of(String i18nValue) {
        return new Option() {
            @Override
            public String value() {
                return i18nValue;
            }

            @Override
            public String[] i18n() {
                return new String[]{i18nValue, "en:" + i18nValue};
            }
        };
    }

    static Option of(String value, String[] i18n) {
        return new Option() {
            @Override
            public String value() {
                return value;
            }

            @Override
            public String[] i18n() {
                return i18n;
            }
        };
    }
}
