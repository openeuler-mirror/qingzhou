package qingzhou.dto.meta.annotation;

import java.util.ArrayList;
import java.util.List;

public class Model extends Base {
    public String className;

    public String icon;

    public String menu;

    public int order;

    public String action; // 页面点击默认操作

    public String[] name;

    public String[] info;

    public final List<ModelField> fields = new ArrayList<>();

    public final List<ModelAction> actions = new ArrayList<>();
}
