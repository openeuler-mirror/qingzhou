package qingzhou.registry.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

public class AppStubRemoteImplTest {
    private static final String APP_CODE = "demo";
    private static final String MODEL_CODE = "jvm";
    private static final String ACTION_CODE = "page";

    @Test
    public void actionRoleNotMatched_remoteInvoke_deniedBeforeForwarding() throws Throwable {
        StubHttpClient httpClient = new StubHttpClient();
        AppStubRemoteImpl appStub = newAppStub(appMeta("action-admin"), httpClient);

        try {
            appStub.invokeApp(request(), new String[]{"reader"});
            Assert.fail("expected denial for caller without required role");
        } catch (IllegalAccessException e) {
            Assert.assertNull(httpClient.sentRequest);
        }
    }

    @Test
    public void actionRoleMatched_remoteInvoke_forwarded() throws Throwable {
        StubHttpClient httpClient = new StubHttpClient();
        AppStubRemoteImpl appStub = newAppStub(appMeta("action-admin"), httpClient);

        appStub.invokeApp(request(), new String[]{"action-admin"});

        Assert.assertNotNull(httpClient.sentRequest);
    }

    @Test
    public void rolesNotDeclared_remoteInvoke_forwarded() throws Throwable {
        StubHttpClient httpClient = new StubHttpClient();
        AppStubRemoteImpl appStub = newAppStub(appMeta(null), httpClient);

        appStub.invokeApp(request(), null);

        Assert.assertNotNull(httpClient.sentRequest);
    }

    private static AppStubRemoteImpl newAppStub(AppMeta appMeta, HttpClient httpClient) {
        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.setId("remote-instance-1");
        instanceInfo.setHost("127.0.0.1");
        instanceInfo.setPort(7900);
        instanceInfo.setKey("remote-key");
        return new AppStubRemoteImpl(instanceInfo, appMeta, new StubJson(), httpClient, crypto(), stub(Logger.class));
    }

    private static Crypto crypto() {
        return (Crypto) Proxy.newProxyInstance(
                AppStubRemoteImplTest.class.getClassLoader(),
                new Class<?>[]{Crypto.class},
                (proxy, method, args) -> {
                    if ("getCipher".equals(method.getName())) return stub(Cipher.class);
                    return defaultValue(proxy, method, args);
                });
    }

    private static RequestImpl request() {
        RequestImpl request = new RequestImpl();
        request.setInstance("remote-instance-1");
        request.setApp(APP_CODE);
        request.setModel(MODEL_CODE);
        request.setAction(ACTION_CODE);
        return request;
    }

    private static AppMeta appMeta(String actionRoles) {
        ModelAction action = new ModelAction();
        action.code = ACTION_CODE;
        action.roles = actionRoles;

        Model model = new Model();
        model.code = MODEL_CODE;
        model.actions.add(action);

        App app = new App();
        app.code = APP_CODE;
        app.models.add(model);

        AppMeta appMeta = new AppMeta();
        appMeta.setApp(app);
        return appMeta;
    }

    private static <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(
                AppStubRemoteImplTest.class.getClassLoader(),
                new Class<?>[]{type},
                AppStubRemoteImplTest::defaultValue));
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

    private static final class StubHttpClient implements HttpClient {
        private Request sentRequest;

        @Override
        public Response send(Request request) {
            sentRequest = request;
            return stub(Response.class);
        }

        @Override
        public Response send(Request request, ResponseListener listener) {
            return send(request);
        }

        @Override
        public Request newRequest(String url) {
            return (Request) Proxy.newProxyInstance(
                    AppStubRemoteImplTest.class.getClassLoader(),
                    new Class<?>[]{Request.class},
                    (proxy, method, args) -> method.getReturnType() == Request.class ? proxy : defaultValue(proxy, method, args));
        }
    }
}
