package qingzhou.dto.meta.annotation;

import java.util.LinkedHashSet;
import java.util.Set;

public class Model extends Base {
    public String roles;

    public String className;

    public String icon;

    public String menu;

    public int order;

    public String[] name;

    public String[] info;

    // 固定顺序：qingzhou.agent.Heartbeat.fingerprints 计算指纹
    public final Set<ModelField> fields = new LinkedHashSet<>();
    public final Set<ModelAction> actions = new LinkedHashSet<>();
}
