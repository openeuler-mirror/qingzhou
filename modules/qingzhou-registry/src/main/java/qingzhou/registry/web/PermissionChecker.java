package qingzhou.registry.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.registry.Constants;

public class PermissionChecker {
    public static boolean isAllowed(App targetApp, String targetModel, String targetAction, String[] userRoles) {
        if (targetApp == null) return false; // 元数据缺失时不得放行

        // 检查应用级权限
        if (!matchesRoles(userRoles, targetApp.roles)) return false;

        for (Model model : targetApp.models) {
            if (!model.code.equals(targetModel)) continue;

            // 检查模块级权限：未声明则继承应用级
            if (!matchesRoles(userRoles, firstDeclared(model.roles, targetApp.roles))) return false;

            for (ModelAction action : model.actions) {
                if (!action.code.equals(targetAction)) continue;

                // 检查操作级权限：未声明则逐级继承（action → model → app）
                return matchesRoles(userRoles, firstDeclared(action.roles, model.roles, targetApp.roles));
            }
            return false;
        }

        return Constants.SYSTEM_MODEL_CODE.equals(targetModel);
    }

    // 返回第一个已声明的角色（fail-closed：全未声明则返回 null，由 matchesRoles 拒绝）。
    // 继承让角色只需在 App 级声明一次，model/action 默认跟随
    private static String firstDeclared(String... declarations) {
        for (String declaration : declarations) {
            if (declaration != null && !declaration.trim().isEmpty()) return declaration;
        }
        return null;
    }

    private static boolean matchesRoles(String[] actual, String declaration) {
        // 未声明即拒绝：放行会让「忘了声明角色」静默变成「所有人可操作」
        if (declaration == null || declaration.trim().isEmpty()) return false;

        Set<String> required = Arrays.stream(declaration.trim().split(","))
                .map(String::trim).filter(role -> !role.isEmpty()).collect(Collectors.toSet());
        if (required.isEmpty()) return false;

        // 显式声明 * 表示任意已认证用户（有主体即通过，不要求具体角色）
        if (required.contains("*")) return actual != null;

        if (actual == null) return false;

        return !Collections.disjoint(required, Arrays.asList(actual));
    }
}
