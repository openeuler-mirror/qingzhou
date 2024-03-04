package qingzhou.api.metadata;

public interface ModelActionData {
    String name();

    String icon();

    String[] nameI18n();

    String[] infoI18n();

    String effectiveWhen();

    String forwardToPage();

    boolean showToListHead();

    boolean showToList();

    int orderOnList();

    boolean supportBatch();

    boolean disabled();
}
