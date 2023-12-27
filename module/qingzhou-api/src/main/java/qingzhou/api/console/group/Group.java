package qingzhou.api.console.group;

public interface Group {
    String GROUP_NAME_BASIC = "basic";

    String name();

    String[] i18n();

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
