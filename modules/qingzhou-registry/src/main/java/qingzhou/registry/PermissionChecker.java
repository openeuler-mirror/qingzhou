package qingzhou.registry;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

/**
 * 应用动作的角色权限判定。
 * 判定逻辑与 HTTP 解耦，供 HTTP 入口、AI / MCP 工具通道与 AppStub 执行入口共用，避免规则漂移。
 */
public class PermissionChecker {
    /**
     * HTTP 入口使用：判定不通过时写入 403 响应
     */
    public boolean checkPermission(HttpRequest httpRequest, HttpResponse httpResponse, AppStub appStub, RequestImpl request) {
        String[] roles = (String[]) httpRequest.getAttribute(AuthResult.AUTH_ROLES_ATTRIBUTE);
        if (isAllowed(roles, appStub.getAppMeta(), request.getModel(), request.getAction())) return true;

        httpResponse.status(403).sendFinish("Forbidden");
        return false;
    }

    /**
     * 纯判定：调用者角色必须由服务端鉴权结果提供，不得取自请求参数
     */
    public boolean isAllowed(String[] roles, AppMeta appMeta, String modelCode, String actionCode) {
        App app = appMeta.getApp();

        // 检查应用级权限
        if (!matchesRoles(roles, app.roles)) return false;

        for (Model model : app.models) {
            if (!model.code.equals(modelCode)) continue;

            // 检查模块级权限
            if (!matchesRoles(roles, model.roles)) return false;

            for (ModelAction action : model.actions) {
                if (!action.code.equals(actionCode)) continue;

                // 检查操作级权限
                return matchesRoles(roles, action.roles);
            }
            break;
        }

        return true;
    }

    private boolean matchesRoles(String[] actual, String declaration) {
        if (declaration == null) return true;

        Set<String> required = Arrays.stream(declaration.split(","))
                .map(String::trim).filter(role -> !role.isEmpty()).collect(Collectors.toSet());
        if (required.isEmpty()) return true;

        if (actual == null) return false;

        return !Collections.disjoint(required, Arrays.asList(actual));
    }
}
