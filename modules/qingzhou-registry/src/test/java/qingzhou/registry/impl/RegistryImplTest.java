package qingzhou.registry.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.api.AppContext;
import qingzhou.api.Constants;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.dto.meta.annotation.App;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStub;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.AppStubRemote;

public class RegistryImplTest {
    private static final String VERSION_PROPERTY = "qingzhou.version";
    private static final String APP_CODE_1 = "app-one";
    private static final String APP_CODE_2 = "app-two";
    private static final String REMOTE_INSTANCE_ID = "remote-instance-1";

    // ===================== 本地应用注册与注销 =====================

    @Test
    public void bindApp_localApp_getLocalAppReturnsStub() {
        RegistryImpl registry = newRegistry();
        AppStubLocal stub = newLocalApp(APP_CODE_1);

        registry.bindApp(stub);

        Assert.assertEquals(registry.getLocalApp(APP_CODE_1), stub);
        Assert.assertTrue(registry.getAllLocalApps().contains(APP_CODE_1));
        Assert.assertEquals(registry.getAllLocalApps().size(), 1);
    }

    @Test
    public void unbindApp_localApp_getLocalAppReturnsNull() {
        RegistryImpl registry = newRegistry();
        AppStubLocal stub = newLocalApp(APP_CODE_1);
        registry.bindApp(stub);

        registry.unbindApp(stub);

        Assert.assertNull(registry.getLocalApp(APP_CODE_1));
        Assert.assertFalse(registry.getAllLocalApps().contains(APP_CODE_1));
        Assert.assertTrue(registry.getAllLocalApps().isEmpty());
    }

    @Test
    public void bindAppDuplicateCode_localApp_lastRegistrationWins() {
        RegistryImpl registry = newRegistry();
        AppStubLocal first = newLocalApp(APP_CODE_1);
        AppStubLocal second = newLocalApp(APP_CODE_1);
        registry.bindApp(first);

        registry.bindApp(second);

        Assert.assertEquals(registry.getLocalApp(APP_CODE_1), second);
        Assert.assertNotSame(registry.getLocalApp(APP_CODE_1), first);
        Assert.assertEquals(registry.getAllLocalApps().size(), 1);
    }

    @Test
    public void bindAndUnbindApp_registryDataVersion_changed() throws InterruptedException {
        RegistryImpl registry = newRegistry();
        AppStubLocal stub = newLocalApp(APP_CODE_1);
        long beforeBind = registry.getRegistryDataVersion();

        Thread.sleep(10);
        registry.bindApp(stub);
        long afterBind = registry.getRegistryDataVersion();

        Thread.sleep(10);
        registry.unbindApp(stub);
        long afterUnbind = registry.getRegistryDataVersion();

        Assert.assertTrue(afterBind > beforeBind);
        Assert.assertTrue(afterUnbind > afterBind);
    }

    @Test
    public void unregisteredCode_getLocalApp_returnNull() {
        RegistryImpl registry = newRegistry();

        Assert.assertNull(registry.getLocalApp(APP_CODE_1));
    }

    // ===================== 远程实例注册与注销 =====================

    @Test
    public void addRemoteApps_instance_getRemoteInstanceReturns() {
        RegistryImpl registry = newRegistry();
        InstanceInfo remote = newRemoteInstance(REMOTE_INSTANCE_ID, APP_CODE_1);

        registry.addRemoteApps(remote);

        Assert.assertEquals(registry.getRemoteInstance(REMOTE_INSTANCE_ID), remote);
        Assert.assertTrue(registry.getAllRemoteInstances().contains(REMOTE_INSTANCE_ID));
    }

    @Test
    public void addRemoteAppsNewInstanceId_registryDataVersion_changed() throws InterruptedException {
        RegistryImpl registry = newRegistry();
        InstanceInfo remote = newRemoteInstance(REMOTE_INSTANCE_ID, APP_CODE_1);
        long before = registry.getRegistryDataVersion();

        Thread.sleep(10);
        registry.addRemoteApps(remote);

        Assert.assertTrue(registry.getRegistryDataVersion() > before);
        Assert.assertTrue(remote.getLastRefreshTime() > 0);
    }

    @Test
    public void addRemoteAppsExistingInstanceId_registryDataVersion_unchanged() throws InterruptedException {
        RegistryImpl registry = newRegistry();
        InstanceInfo first = newRemoteInstance(REMOTE_INSTANCE_ID, APP_CODE_1);
        Thread.sleep(10);
        registry.addRemoteApps(first);
        long versionAfterFirst = registry.getRegistryDataVersion();

        Thread.sleep(10);
        InstanceInfo second = newRemoteInstance(REMOTE_INSTANCE_ID, APP_CODE_2);
        InstanceInfo exists = registry.addRemoteApps(second);

        Assert.assertEquals(exists, first);
        Assert.assertEquals(registry.getRegistryDataVersion(), versionAfterFirst);
        Assert.assertEquals(registry.getRemoteInstance(REMOTE_INSTANCE_ID), second);
        // 重复注册时实现不调用 setLastRefreshTime，新实例的 lastRefreshTime 保持默认值 0
        Assert.assertEquals(second.getLastRefreshTime(), 0);
        Assert.assertTrue(first.getLastRefreshTime() > 0);
    }

    @Test
    public void removeRemoteApps_instance_getRemoteInstanceReturnsNull() throws InterruptedException {
        RegistryImpl registry = newRegistry();
        InstanceInfo remote = newRemoteInstance(REMOTE_INSTANCE_ID, APP_CODE_1);
        Thread.sleep(10);
        registry.addRemoteApps(remote);
        long before = registry.getRegistryDataVersion();

        Thread.sleep(10);
        registry.removeRemoteApps(REMOTE_INSTANCE_ID);

        Assert.assertNull(registry.getRemoteInstance(REMOTE_INSTANCE_ID));
        Assert.assertFalse(registry.getAllRemoteInstances().contains(REMOTE_INSTANCE_ID));
        Assert.assertTrue(registry.getRegistryDataVersion() > before);
    }

    @Test
    public void unregisteredInstanceId_getRemoteInstance_returnNull() {
        RegistryImpl registry = newRegistry();

        Assert.assertNull(registry.getRemoteInstance(REMOTE_INSTANCE_ID));
    }

    // ===================== 远程应用列表与远程 AppStub =====================

    @Test
    public void getAllRemoteApps_registeredInstance_returnAppCodes() {
        RegistryImpl registry = newRegistry();
        registry.addRemoteApps(newRemoteInstance(REMOTE_INSTANCE_ID, APP_CODE_1, APP_CODE_2));

        List<String> appCodes = registry.getAllRemoteApps(REMOTE_INSTANCE_ID);

        Assert.assertNotNull(appCodes);
        Assert.assertEquals(appCodes.size(), 2);
        Assert.assertTrue(appCodes.contains(APP_CODE_1));
        Assert.assertTrue(appCodes.contains(APP_CODE_2));
    }

    @Test
    public void getAllRemoteApps_unregisteredInstanceId_returnNull() {
        RegistryImpl registry = newRegistry();

        Assert.assertNull(registry.getAllRemoteApps(REMOTE_INSTANCE_ID));
    }

    @Test
    public void getRemoteApp_registeredInstanceAndCode_returnStub() {
        RegistryImpl registry = newRegistry();
        registry.addRemoteApps(newRemoteInstance(REMOTE_INSTANCE_ID, APP_CODE_1));

        AppStubRemote remoteApp = registry.getRemoteApp(REMOTE_INSTANCE_ID, APP_CODE_1);

        Assert.assertNotNull(remoteApp);
        Assert.assertNotNull(remoteApp.getAppMeta());
        Assert.assertEquals(remoteApp.getAppMeta().getApp().code, APP_CODE_1);
    }

    @Test
    public void getRemoteApp_unknownCode_returnNull() {
        RegistryImpl registry = newRegistry();
        registry.addRemoteApps(newRemoteInstance(REMOTE_INSTANCE_ID, APP_CODE_1));

        Assert.assertNull(registry.getRemoteApp(REMOTE_INSTANCE_ID, APP_CODE_2));
    }

    @Test
    public void getRemoteApp_unknownInstanceId_returnNull() {
        RegistryImpl registry = newRegistry();

        Assert.assertNull(registry.getRemoteApp(REMOTE_INSTANCE_ID, APP_CODE_1));
    }

    // ===================== getAppStub 路由 =====================

    @Test
    public void getAppStub_localInstanceId_returnLocalAppStub() {
        RegistryImpl registry = newRegistry();
        AppStubLocal stub = newLocalApp(APP_CODE_1);
        registry.bindApp(stub);

        AppStub result = registry.getAppStub(Constants.LOCAL_INSTANCE_ID, APP_CODE_1);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof AppStubLocal);
        Assert.assertEquals(result, stub);
    }

    @Test
    public void getAppStub_remoteInstanceId_returnRemoteAppStub() {
        RegistryImpl registry = newRegistry();
        registry.addRemoteApps(newRemoteInstance(REMOTE_INSTANCE_ID, APP_CODE_1));

        AppStub result = registry.getAppStub(REMOTE_INSTANCE_ID, APP_CODE_1);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof AppStubRemote);
    }

    @Test
    public void getAppStub_localInstanceIdUnknownCode_returnNull() {
        RegistryImpl registry = newRegistry();

        Assert.assertNull(registry.getAppStub(Constants.LOCAL_INSTANCE_ID, APP_CODE_1));
    }

    @Test
    public void getAppStub_remoteInstanceIdUnknownCode_returnNull() {
        RegistryImpl registry = newRegistry();
        registry.addRemoteApps(newRemoteInstance(REMOTE_INSTANCE_ID, APP_CODE_1));

        Assert.assertNull(registry.getAppStub(REMOTE_INSTANCE_ID, APP_CODE_2));
    }

    // ===================== start 与 getLocalInstance =====================

    @Test
    public void start_validVersion_propertyParsed() throws Exception {
        RegistryImpl registry = newRegistry();
        System.setProperty(VERSION_PROPERTY, "version1.2.3");
        try {
            registry.start();
        } finally {
            System.clearProperty(VERSION_PROPERTY);
        }

        Assert.assertEquals(getQzVersion(registry), "1.2.3");
    }

    @Test
    public void start_missingVersionProperty_throwException() {
        RegistryImpl registry = newRegistry();

        try {
            registry.start();
            Assert.fail("缺失 qingzhou.version 系统属性应抛出异常");
        } catch (Throwable e) {
            Assert.assertNotNull(e);
        try {
            registry.start();
            Assert.fail("缺失 qingzhou.version 系统属性应抛出异常");
        } catch (NullPointerException expected) {
            // 预期：System.getProperty 返回 null 后，new File(null) 构造触发 NPE
        }
    }

    @Test
    public void getLocalInstance_calledTwice_returnSameInstance() {
        RegistryImpl registry = newRegistryWithConfig();

        InstanceInfo first = registry.getLocalInstance();
        InstanceInfo second = registry.getLocalInstance();

        Assert.assertSame(first, second);
        Assert.assertEquals(first.getId(), Constants.LOCAL_INSTANCE_ID);
        Assert.assertEquals(first.getHost(), "localhost");
    }

    @Test
    public void getLocalInstance_appMetas_matchLocalApps() {
        RegistryImpl registry = newRegistryWithConfig();
        registry.bindApp(newLocalApp(APP_CODE_1));
        registry.bindApp(newLocalApp(APP_CODE_2));

        List<AppMeta> appMetas = registry.getLocalInstance().getAppMetas();

        Assert.assertEquals(appMetas.size(), 2);
        boolean hasApp1 = appMetas.stream().anyMatch(m -> m.getApp().code.equals(APP_CODE_1));
        boolean hasApp2 = appMetas.stream().anyMatch(m -> m.getApp().code.equals(APP_CODE_2));
        Assert.assertTrue(hasApp1);
        Assert.assertTrue(hasApp2);
    }

    // ===================== 辅助方法 =====================

    private RegistryImpl newRegistry() {
        RegistryImpl registry = new RegistryImpl();
        setField(registry, "logger", newLoggerStub());
        return registry;
    }

    private RegistryImpl newRegistryWithConfig() {
        RegistryImpl registry = newRegistry();
        setField(registry, "configAdmin", newConfigAdminStub(7900));
        return registry;
    }

    private AppStubLocal newLocalApp(String appCode) {
        return new StubLocalApp(appCode);
    }

    private InstanceInfo newRemoteInstance(String instanceId, String... appCodes) {
        InstanceInfo info = new InstanceInfo();
        info.setId(instanceId);
        info.setHost("remote-host");
        info.setPort(7900);
        info.setKey("remote-key");
        info.setVersion("version1.0");
        for (String code : appCodes) {
            AppMeta meta = new AppMeta();
            App app = new App();
            app.code = code;
            meta.setApp(app);
            info.getAppMetas().add(meta);
        }
        return info;
    }

    private static String getQzVersion(RegistryImpl registry) throws Exception {
        Field field = RegistryImpl.class.getDeclaredField("qzVersion");
        field.setAccessible(true);
        return (String) field.get(registry);
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Logger newLoggerStub() {
        return (Logger) Proxy.newProxyInstance(
                RegistryImplTest.class.getClassLoader(),
                new Class<?>[]{Logger.class},
                (proxy, method, args) -> {
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    return null;
                });
    }

    private static ConfigurationAdmin newConfigAdminStub(int port) {
        return (ConfigurationAdmin) Proxy.newProxyInstance(
                RegistryImplTest.class.getClassLoader(),
                new Class<?>[]{ConfigurationAdmin.class},
                (proxy, method, args) -> {
                    if ("getConfiguration".equals(method.getName())) {
                        return newConfigurationStub(port);
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    return null;
                });
    }

    private static Configuration newConfigurationStub(int port) {
        return (Configuration) Proxy.newProxyInstance(
                RegistryImplTest.class.getClassLoader(),
                new Class<?>[]{Configuration.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getProperties".equals(name)) {
                        Dictionary<String, Object> props = new Hashtable<>();
                        props.put("port", String.valueOf(port));
                        return props;
                    }
                    if ("getPid".equals(name)) return "qingzhou-http-server";
                    if ("getFactoryPid".equals(name)) return null;
                    if ("getBundleLocation".equals(name)) return null;
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    return null;
                });
    }

    private static final class StubLocalApp implements AppStubLocal {
        private final AppMeta appMeta;

        StubLocalApp(String appCode) {
            App app = new App();
            app.code = appCode;
            this.appMeta = new AppMeta();
            this.appMeta.setApp(app);
        }

        @Override
        public AppMeta getAppMeta() {
            return appMeta;
        }

        @Override
        public void invokeApp(RequestImpl request) {
        }

        @Override
        public AppContext getAppContext() {
            return null;
        }
    }
}
