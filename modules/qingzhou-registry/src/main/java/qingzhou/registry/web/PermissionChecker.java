package qingzhou.registry.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.registry.AppStub;

class PermissionChecker {

    boolean checkPermission(HttpRequest httpRequest, HttpResponse httpResponse, AppStub appStub, RequestImpl request) {
        if (isAllowed(httpRequest, appStub, request)) return true;

        httpResponse.status(403).sendFinish("Forbidden");
        return false;
    }

    private boolean isAllowed(HttpRequest httpRequest, AppStub appStub, RequestImpl request) {
        String[] roles = (String[]) httpRequest.getAttribute(AuthResult.AUTH_ROLES_ATTRIBUTE);
        App app = appStub.getAppMeta().getApp();

        // 检查应用级权限
        if (!matchesRoles(roles, app.roles)) return false;

        for (Model model : app.models) {
            if (!model.code.equals(request.getModel())) continue;

            // 检查模块级权限
            if (!matchesRoles(roles, model.roles)) return false;

            for (ModelAction action : model.actions) {
                if (!action.code.equals(request.getAction())) continue;

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
