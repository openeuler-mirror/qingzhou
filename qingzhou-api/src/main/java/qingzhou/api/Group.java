package qingzhou.api;

import java.util.Arrays;

public interface Group {
    String name();

    String[] i18n();

    static Group of(String name) {
        return new Group() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String[] i18n() {
                return Arrays.stream(Lang.values()).map(lang -> lang.name() + Lang.SEPARATOR + name).toArray(String[]::new);
            }
        };
    }

    static Group of(String name, String[] i18n) {
        return new Group() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String[] i18n() {
                return i18n;
            }
        };
    }
}
