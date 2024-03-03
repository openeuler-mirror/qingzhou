package qingzhou.api;

import java.util.Arrays;

public interface Option {
    String value();

    String[] i18n();

    static Option of(String value) {
        return new Option() {
            @Override
            public String value() {
                return value;
            }

            @Override
            public String[] i18n() {
                return Arrays.stream(Lang.values()).map(lang -> lang.name() + Lang.SEPARATOR + value).toArray(String[]::new);
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
