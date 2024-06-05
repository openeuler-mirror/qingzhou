package qingzhou.app.master;

import qingzhou.api.AppContext;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;

import java.io.File;

@qingzhou.api.App
public class MasterApp extends QingzhouSystemApp {
    private static MasterApp masterApp;

    @Override
    public void start(AppContext appContext) {
        masterApp = this;

        appContext.addI18n("validator.exist", new String[]{"已存在", "en:Already exists"});
        appContext.addI18n("validator.require", new String[]{"不支持为空", "en:Cannot be empty"});
        appContext.addI18n("confirmPassword.different", new String[]{"输入的确认密码与密码不一致", "en:Confirm that the password does not match the new password"});
        appContext.addI18n("password.format", new String[]{"密码须包含大小写字母、数字、特殊符号，长度至少 10 位。", "en:Password must contain uppercase and lowercase letters, numbers, special symbols, and must be at least 10 characters long"});
        appContext.addI18n("password.passwordContainsUsername", new String[]{"密码不能包含用户名", "en:A weak password, the password cannot contain the username"});
        appContext.addI18n("password.continuousChars", new String[]{"密码不能包含三个或三个以上相同或连续的字符", "en:A weak password, the password cannot contain three or more same or consecutive characters"});
        appContext.addI18n("password.lengthBetween", new String[]{"密码长度必须介于%s - %s之间", "en:Password length must be between %s and %s"});
        appContext.addI18n("validator.master.system", new String[]{"为保障系统安全可用，请勿修改此配置", "en:To ensure the security and availability of the system, do not modify this configuration"});
        appContext.addI18n("app.delete.notlocal", new String[]{"不能卸载其他实例上部署的应用", "en:You can't uninstall apps deployed on other instances"});
        appContext.addMenu("Service", new String[]{"服务管理", "en:Service"}, "th-large", 1);
        appContext.addMenu("System", new String[]{"系统管理", "en:System"}, "cog", 2);
    }

    public static File getInstanceDir() {
        return masterApp.moduleContext.getInstanceDir();
    }

    public static File getLibDir() {
        return masterApp.moduleContext.getLibDir();
    }

    public static <T> T getService(Class<T> type) {
        if (type == ModuleContext.class) return (T) masterApp.moduleContext;

        return masterApp.moduleContext.getService(type);
    }
}
