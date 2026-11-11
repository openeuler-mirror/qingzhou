package qingzhou.dto.meta;

import java.util.LinkedHashSet;

public class App extends Base {
    public String roles = "";
    public String className;
    public String[] name;
    public String[] info = new String[0];
    public String icon = "";

    // 固定顺序：qingzhou.agent.Heartbeat.fingerprints 计算指纹
    public final LinkedHashSet<Model> models = new LinkedHashSet<>();
    public final LinkedHashSet<Menu> menus = new LinkedHashSet<>();
    public final LinkedHashSet<I18n> i18ns = new LinkedHashSet<>();
}
