package qingzhou.dto.meta;

import java.util.LinkedHashSet;

public class Model extends Base {
    public String roles = "";

    public String className;

    public String icon = "";

    public String menu = "";

    public int order = 9;

    public String[] name;

    public String[] info = new String[0];

    // 固定顺序：qingzhou.agent.Heartbeat.fingerprints 计算指纹
    // 必须声明为有序实现：Jackson 对 Set 接口默认实例化 HashSet，会按 code 哈希重排。
    public final LinkedHashSet<ModelField> fields = new LinkedHashSet<>();
    public final LinkedHashSet<ModelAction> actions = new LinkedHashSet<>();
}
