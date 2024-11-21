package qingzhou.api;

public interface Menu {
    Menu icon(String icon);

    Menu order(int order);

    Menu parent(String parent);

    Menu model(String model);

    Menu action(String action);

}
