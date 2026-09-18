package qingzhou.registry.ai;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import qingzhou.ai.ToolService;
import qingzhou.api.AppContext;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Registry;
import qingzhou.registry.web.WebUtil;

public class AppActionToolsTest {
    private static final String PERMISSION_DENIED = "无权限：当前用户不具备执行该操作所需的角色。";
    private static final String APP_CODE = "demo";
    private static final String MODEL_CODE = "jvm";
    private static final String PAGE_TOOL = "app_action_page";

    private final Map<String, ToolService> registeredTools = new HashMap<>();
    private StubApp stubApp;

    @BeforeMethod
    public void setUp() throws Exception {
        stubApp = new StubApp();
        AppActionTools appActionTools = new AppActionTools();
        setField(appActionTools, "registry", registry());
        setField(appActionTools, "logger", stub(Logger.class));
        setField(appActionTools, "json", new StubJson());
        appActionTools.init(componentContext());
    }

    @Test
    public void actionRoleNotMatched_toolCall_deniedWithoutInvokingApp() throws Exception {
        stubApp.appMeta = appMeta(null, null, "action-admin");

        String result = registeredTools.get(PAGE_TOOL).invoke(toolArgs(), new String[]{"reader"});

        Assert.assertEquals(result, PERMISSION_DENIED);
        Assert.assertNull(stubApp.invokedRequest);
    }

    @Test
    public void rolesAbsent_toolCall_deniedWithoutInvokingApp() throws Exception {
        stubApp.appMeta = appMeta("admin", "model-admin", "action-admin");

        String result = registeredTools.get(PAGE_TOOL).invoke(toolArgs(), null);

        Assert.assertEquals(result, PERMISSION_DENIED);
        Assert.assertNull(stubApp.invokedRequest);
    }

    @Test
    public void actionRoleMatched_toolCall_invokesApp() throws Exception {
        stubApp.appMeta = appMeta(null, null, "action-admin");

        String result = registeredTools.get(PAGE_TOOL).invoke(toolArgs(), new String[]{"action-admin"});

        Assert.assertEquals(result, "{}");
        Assert.assertNotNull(stubApp.invokedRequest);
    }

    @Test
    public void rolesNotDeclared_toolCall_invokesApp() throws Exception {
        stubApp.appMeta = appMeta(null, null, null);

        registeredTools.get(PAGE_TOOL).invoke(toolArgs(), null);

        Assert.assertNotNull(stubApp.invokedRequest);
    }

    private static Map<String, Object> toolArgs() {
        Map<String, Object> toolArgs = new HashMap<>();
        toolArgs.put(WebUtil.INSTANCE_ID, "local-instance");
        toolArgs.put(WebUtil.APP_CODE, APP_CODE);
        toolArgs.put(WebUtil.MODEL_CODE, MODEL_CODE);
        return toolArgs;
    }

    private static AppMeta appMeta(String appRoles, String modelRoles, String actionRoles) {
        ModelAction action = new ModelAction();
        action.code = "page";
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

    private Registry registry() {
        return (Registry) Proxy.newProxyInstance(
                AppActionToolsTest.class.getClassLoader(),
                new Class<?>[]{Registry.class},
                (proxy, method, args) -> {
                    if ("getAppStub".equals(method.getName())) return stubApp;
                    return defaultValue(proxy, method, args);
                });
    }

    private ComponentContext componentContext() {
        BundleContext bundleContext = (BundleContext) Proxy.newProxyInstance(
                AppActionToolsTest.class.getClassLoader(),
                new Class<?>[]{BundleContext.class},
                (proxy, method, args) -> {
                    if ("registerService".equals(method.getName())) {
                        Dictionary<?, ?> properties = (Dictionary<?, ?>) args[2];
                        registeredTools.put(String.valueOf(properties.get(ToolService.TOOL_NAME)), (ToolService) args[1]);
                        return stub(ServiceRegistration.class);
                    }
                    return defaultValue(proxy, method, args);
                });
        return (ComponentContext) Proxy.newProxyInstance(
                AppActionToolsTest.class.getClassLoader(),
                new Class<?>[]{ComponentContext.class},
                (proxy, method, args) -> {
                    if ("getBundleContext".equals(method.getName())) return bundleContext;
                    return defaultValue(proxy, method, args);
                });
    }

    private static <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(
                AppActionToolsTest.class.getClassLoader(),
                new Class<?>[]{type},
                AppActionToolsTest::defaultValue));
    }

    private static Object defaultValue(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("toString".equals(name)) return "stub";
        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        if ("equals".equals(name)) return args != null && args.length > 0 && proxy == args[0];

        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class StubApp implements AppStubLocal {
        private AppMeta appMeta;
        private RequestImpl invokedRequest;

        @Override
        public AppMeta getAppMeta() {
            return appMeta;
        }

        @Override
        public void invokeApp(RequestImpl request) {
            invokedRequest = request;
        }

        @Override
        public AppContext getAppContext() {
            return null;
        }
    }

    private static final class StubJson implements Json {
        @Override
        public String toJson(Object src) {
            return "{}";
        }

        @Override
        public <T> T fromJson(String json, Class<T> classOfT) {
            return null;
        }
    }
}
