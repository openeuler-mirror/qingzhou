package qingzhou.framework.api;

public interface AppStub {
    ModelManager getModelManager();

    String getI18N(Lang lang, String key, Object... args);

    MenuInfo getMenuInfo(String menuName);
}
