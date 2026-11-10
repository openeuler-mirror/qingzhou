package qingzhou.registry;

import qingzhou.dto.meta.App;

public interface PermissionChecker {
    boolean isAllowed(App targetApp, String targetModel, String targetAction, String[] userRoles);
}
