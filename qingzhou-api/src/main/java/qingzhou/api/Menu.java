package qingzhou.api;

public interface Menu {
    Menu icon(String icon);

    Menu order(String order);

    Menu parent(String parent);

    // 展开菜单时候执行的 model action
    Menu action(String model, String action);
}
