package qingzhou.registry.monitor;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.registry.Registry;

public class RemoteMetricsProviderTest {

    // 无远端实例时不得触碰通信协作者与线程池：二者均未初始化即不外连的证据
    @Test
    public void noRemoteInstances_collectMetrics_noRemoteAccess() throws Exception {
        RemoteMetricsProvider provider = new RemoteMetricsProvider();
        setField(provider, "registry", registryStub(Collections.emptyList()));
        Assert.assertTrue(provider.collectMetrics().isEmpty());
    }

    private static Registry registryStub(List<String> instances) {
        return (Registry) Proxy.newProxyInstance(
                RemoteMetricsProviderTest.class.getClassLoader(), new Class<?>[]{Registry.class},
                (proxy, method, args) -> {
                    if ("getAllRemoteInstances".equals(method.getName())) return instances;
                    Class<?> type = method.getReturnType();
                    if (type == long.class) return 0L;
                    if (type == int.class) return 0;
                    if (type == boolean.class) return false;
                    return null;
                });
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
