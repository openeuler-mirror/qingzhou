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

/**
 * 应用动作的角色权限判定：由应用动作的统一执行入口调用，
 * 供 HTTP 入口与 AI / MCP 工具通道共用，避免规则漂移。
 */
public class PermissionChecker {
    // 判定并回写结果：无权限时不执行动作，结果与 HTTP 入口原有的 403 语义一致
    public boolean checkPermission(String[] roles, AppMeta appMeta, RequestImpl request) {
        if (isAllowed(roles, appMeta, request.getModel(), request.getAction())) return true;

        request.getResponse().status(403).error("Forbidden");
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
