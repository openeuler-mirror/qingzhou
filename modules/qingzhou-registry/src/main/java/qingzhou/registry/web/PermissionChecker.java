package qingzhou.registry.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;

public class PermissionChecker {
    public static boolean isAllowed(App targetApp, String targetModel, String targetAction, String[] userRoles) {
        // 检查应用级权限
        if (!matchesRoles(userRoles, targetApp.roles)) return false;

        for (Model model : targetApp.models) {
            if (!model.code.equals(targetModel)) continue;

            // 检查模块级权限
            if (!matchesRoles(userRoles, model.roles)) return false;

            for (ModelAction action : model.actions) {
                if (!action.code.equals(targetAction)) continue;

                // 检查操作级权限
                return matchesRoles(userRoles, action.roles);
            }
            break;
        }

        return true;
    }

    private static boolean matchesRoles(String[] actual, String declaration) {
        if (declaration == null) return true;

        Set<String> required = Arrays.stream(declaration.split(","))
                .map(String::trim).filter(role -> !role.isEmpty()).collect(Collectors.toSet());
        if (required.isEmpty()) return true;

        if (actual == null) return false;

        return !Collections.disjoint(required, Arrays.asList(actual));
    }
}
