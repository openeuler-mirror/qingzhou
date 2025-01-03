package qingzhou.core.deployer.impl;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Visitor;

/**
 * hack into ModuleContextImpl:
 * 因底层服务管理模块约束，未经过 @Resource 注入的服务无法通过 getService(xx) 使用，因此，
 * 为了使得应用可以依赖到插件里自定义的服务，强行修改底层服务管理的注册表
 */
class HackUtil {
    private static Field moduleInfoField;
    private static Field engineContextField;
    private static Field moduleInfoListField;
    private static Field moduleContextField;
    private static Field registeredServicesField;
    private static Field injectedServicesField;

    static void visitAllModuleContext(ModuleContext moduleContext, Visitor<ModuleContext> visitor) throws Throwable {
        if (moduleInfoField == null) {
            moduleInfoField = moduleContext.getClass().getDeclaredField("moduleInfo");
            moduleInfoField.setAccessible(true);
        }
        Object moduleInfo = moduleInfoField.get(moduleContext);

        if (engineContextField == null) {
            engineContextField = moduleInfo.getClass().getDeclaredField("engineContext");
            engineContextField.setAccessible(true);
        }
        Object engineContext = engineContextField.get(moduleInfo);

        if (moduleInfoListField == null) {
            moduleInfoListField = engineContext.getClass().getDeclaredField("moduleInfoList");
            moduleInfoListField.setAccessible(true);
        }
        List moduleInfoList = (List) moduleInfoListField.get(engineContext);

        for (Object otherModuleInfo : moduleInfoList) {
            if (moduleContextField == null) {
                moduleContextField = otherModuleInfo.getClass().getDeclaredField("moduleContext");
                moduleContextField.setAccessible(true);
            }
            ModuleContext context = (ModuleContext) moduleContextField.get(otherModuleInfo);
            if (!visitor.visit(context)) return;
        }
    }

    static Map<Class<?>, Object> getRegisteredServices(ModuleContext moduleContext) throws Exception {
        if (registeredServicesField == null) {
            try {
                registeredServicesField = moduleContext.getClass().getDeclaredField("registeredServices");
                registeredServicesField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        return (Map<Class<?>, Object>) registeredServicesField.get(moduleContext);
    }

    static Map<Class<?>, Object> getInjectedServices(ModuleContext moduleContext) throws Exception {
        if (injectedServicesField == null) {
            try {
                injectedServicesField = moduleContext.getClass().getDeclaredField("injectedServices");
                injectedServicesField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        return (Map<Class<?>, Object>) injectedServicesField.get(moduleContext);
    }
}
