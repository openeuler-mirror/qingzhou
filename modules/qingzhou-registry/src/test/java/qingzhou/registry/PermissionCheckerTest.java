package qingzhou.registry;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;

public class PermissionCheckerTest {
    private static final String APP_CODE = "demo";
    private static final String MODEL_CODE = "jvm";
    private static final String ACTION_CODE = "page";

    private final PermissionChecker checker = new PermissionChecker();

    @Test
    public void rolesNotDeclared_anyCaller_allowed() {
        AppMeta appMeta = appMeta(null, null, null);

        Assert.assertTrue(checker.isAllowed(null, appMeta, MODEL_CODE, ACTION_CODE));
        Assert.assertTrue(checker.isAllowed(new String[]{"reader"}, appMeta, MODEL_CODE, ACTION_CODE));
    }

    @Test
    public void appRoleRequired_roleAbsent_denied() {
        AppMeta appMeta = appMeta("admin", null, null);

        Assert.assertFalse(checker.isAllowed(null, appMeta, MODEL_CODE, ACTION_CODE));
        Assert.assertFalse(checker.isAllowed(new String[]{"reader"}, appMeta, MODEL_CODE, ACTION_CODE));
    }

    @Test
    public void modelRoleRequired_roleNotMatched_denied() {
        AppMeta appMeta = appMeta(null, "model-admin", null);

        Assert.assertFalse(checker.isAllowed(new String[]{"reader"}, appMeta, MODEL_CODE, ACTION_CODE));
    }

    @Test
    public void actionRoleRequired_roleNotMatched_denied() {
        AppMeta appMeta = appMeta(null, null, "action-admin");

        Assert.assertFalse(checker.isAllowed(new String[]{"reader"}, appMeta, MODEL_CODE, ACTION_CODE));
    }

    @Test
    public void allLevelRolesMatched_caller_allowed() {
        AppMeta appMeta = appMeta("app-role", "model-role", "action-role");

        Assert.assertTrue(checker.isAllowed(
                new String[]{"other", "app-role", "model-role", "action-role"}, appMeta, MODEL_CODE, ACTION_CODE));
    }

    @Test
    public void modelNotDeclared_anyCaller_allowed() {
        // 模型未命中时不进入模块与动作级判定，保持既有放行语义
        AppMeta appMeta = appMeta(null, "model-admin", "action-admin");

        Assert.assertTrue(checker.isAllowed(null, appMeta, "unregistered-model", ACTION_CODE));
    }

    private AppMeta appMeta(String appRoles, String modelRoles, String actionRoles) {
        ModelAction action = new ModelAction();
        action.code = ACTION_CODE;
        action.roles = actionRoles;

        Model model = new Model();
        model.code = MODEL_CODE;
        model.roles = modelRoles;
        model.actions.add(action);

        App app = new App();
        app.code = APP_CODE;
        app.roles = appRoles;
        app.models.add(model);

        AppMeta appMeta = new AppMeta();
        appMeta.setApp(app);
        return appMeta;
    }
}
