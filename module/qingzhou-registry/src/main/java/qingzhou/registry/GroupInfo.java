package qingzhou.registry;

import qingzhou.api.Group;

public class GroupInfo {
    private String name;
    private String[] i18n;

    public static GroupInfo other(){
        return new GroupInfo("OTHERS", new String[]{"其他", "en:Other"});
    }

    public GroupInfo(Group group) {
        this.name = group.name();
        this.i18n = group.i18n();
    }

    public GroupInfo(String name, String[] i18n) {
        this.name = name;
        this.i18n = i18n;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getI18n() {
        return i18n;
    }

    public void setI18n(String[] i18n) {
        this.i18n = i18n;
    }
}
