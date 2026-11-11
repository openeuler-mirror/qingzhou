package qingzhou.dto.meta;

public class ModelActionView extends Base {
    public String icon = "";

    public int order = 9;

    public String display = "";

    public boolean add;

    public boolean update;

    public boolean show;

    public boolean list_head;

    public boolean list;

    public boolean batch;

    public String[] name = new String[0];

    public String[] info = new String[0];

    // 确认提示信息，国际化数组。非空时前端在执行操作前弹出确认对话框。
    public String[] confirm = new String[0];
}
