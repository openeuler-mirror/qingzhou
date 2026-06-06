package qingzhou.dto.meta.annotation;

public class ModelActionView extends Base {
    public String icon;

    public int order;

    public String display;

    public boolean add;

    public boolean update;

    public boolean show;

    public boolean list_head;

    public boolean list;

    public boolean batch;

    public String[] name;

    public String[] info;

    // 确认提示信息，国际化数组。非空时前端在执行操作前弹出确认对话框。
    public String[] confirm;
}
