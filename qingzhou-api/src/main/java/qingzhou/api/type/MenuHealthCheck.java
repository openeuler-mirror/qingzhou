package qingzhou.api.type;
/**
 * 菜单健康检查
 */
public interface MenuHealthCheck {
    String ACTION_MENUHEALTHCHECK = "menuHealthCheck";

    String menuHealthCheck() throws Exception;

}
