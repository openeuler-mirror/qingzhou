package qingzhou.registry;

import org.testng.Assert;
import org.testng.annotations.Test;

import qingzhou.dto.meta.App;
import qingzhou.dto.meta.Model;
import qingzhou.dto.meta.ModelAction;
import qingzhou.registry.impl.PermissionCheckerImpl;

public class PermissionCheckerImplTest {
    private static final String[] ADMIN = {"admin"};
    private static final String[] VIEWER = {"viewer"};

    private static App app(String appRoles) {
        App app = new App();
        app.code = "app";
        app.roles = appRoles;

        Model model = new Model();
        model.code = "model";
        model.roles = null; // 未声明：验证继承
        ModelAction action = new ModelAction();
        action.code = "action";
        action.roles = null;
        model.actions.add(action);
        app.models.add(model);
        return app;
    }

    private boolean isAllowed(App targetApp, String targetModel, String targetAction, String[] userRoles) {
        return new PermissionCheckerImpl().isAllowed(targetApp, targetModel, targetAction, userRoles);
    }

    @Test
    public void declaredRole_inheritedByModelAndAction() {
        App app = app("admin");

        Assert.assertTrue(isAllowed(app, "model", "action", ADMIN)); // 继承应用级声明
        Assert.assertFalse(isAllowed(app, "model", "action", VIEWER));
        Assert.assertFalse(isAllowed(app, "model", "action", null)); // 未认证一律拒绝
    }

    @Test
    public void undeclaredRoles_failClosed() {
        App app = app(null); // 应用级也未声明

        Assert.assertFalse(isAllowed(app, "model", "action", ADMIN));
        Assert.assertFalse(isAllowed(app, "model", "action", (String[]) null));
    }

    @Test
    public void actionDeclaredRole_narrowsFurther() {
        App app = app("admin");
        Model model = app.models.iterator().next();
        model.actions.iterator().next().roles = "viewer";

        // 逐级 AND：每层声明都必须满足，子级声明只能收窄不能放宽
        Assert.assertTrue(isAllowed(app, "model", "action", new String[]{"admin", "viewer"}));
        Assert.assertFalse(isAllowed(app, "model", "action", ADMIN)); // 缺 viewer
        Assert.assertFalse(isAllowed(app, "model", "action", VIEWER)); // 缺 admin
    }

    @Test
    public void wildcard_allowsAnyAuthenticatedPrincipal() {
        App app = app("*");

        Assert.assertTrue(isAllowed(app, "model", "action", VIEWER));
        Assert.assertTrue(isAllowed(app, "model", "action", new String[0])); // 已认证但无角色
        Assert.assertFalse(isAllowed(app, "model", "action", null));
    }

    @Test
    public void unknownModelOrAction_failClosed() {
        App app = app("admin");

        Assert.assertFalse(isAllowed(app, "no-such-model", "action", ADMIN));
        Assert.assertFalse(isAllowed(app, "model", "no-such-action", ADMIN));
        Assert.assertFalse(isAllowed(null, "model", "action", ADMIN));
    }

    @Test
    public void systemModel_allowedWhenAppRolesPass() {
        App app = app("admin");

        Assert.assertTrue(isAllowed(app, "-", "action", ADMIN));
        Assert.assertFalse(isAllowed(app, "-", "action", VIEWER)); // 应用级未过，系统模型同样拒绝
    }
}
