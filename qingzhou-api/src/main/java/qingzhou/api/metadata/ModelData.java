package qingzhou.api.metadata;

public interface ModelData {
    String name();

    String icon();

    String[] nameI18n();

    String[] infoI18n();

    String entryAction();

    boolean showToMenu();

    String menuName();

    int menuOrder();
}
