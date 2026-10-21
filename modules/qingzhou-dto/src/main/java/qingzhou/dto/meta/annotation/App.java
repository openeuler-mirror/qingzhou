package qingzhou.dto.meta.annotation;

import java.util.LinkedHashSet;
import java.util.Set;

public class App extends Base {
    public String roles;
    public String className;
    public String[] name;
    public String[] info;
    public String icon;

    // 固定顺序：qingzhou.agent.Heartbeat.fingerprints 计算指纹
    public final Set<Model> models = new LinkedHashSet<>();
    public final Set<Menu> menus = new LinkedHashSet<>();
    public final Set<I18n> i18ns = new LinkedHashSet<>();
}
