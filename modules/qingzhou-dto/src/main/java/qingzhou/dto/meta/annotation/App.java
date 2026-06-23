package qingzhou.dto.meta.annotation;

import java.util.HashSet;
import java.util.Set;

public class App extends Base {
    public String className;
    public String[] name;
    public String[] info;
    public String icon;
    public final Set<Model> models = new HashSet<>();
    public final Set<Menu> menus = new HashSet<>();
    public final Set<Group> groups = new HashSet<>();
}
