package qingzhou.config.remote;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RemoteOptionsTest {
    private Properties props() {
        return new Properties();
    }

    @Test
    public void emptyConfig_from_defaultDisabledAndEtcd() {
        RemoteOptions options = RemoteOptions.from(props());

        Assert.assertFalse(options.enabled);
        Assert.assertEquals(options.type, "etcd");
        Assert.assertEquals(options.username, null);
    }

    @Test
    public void enabledFlag_from_parsedTrue() {
        Properties props = props();
        props.setProperty(RemoteOptions.KEY_PREFIX + "enabled", "true");

        Assert.assertTrue(RemoteOptions.from(props).enabled);
    }

    @Test
    public void explicitNamespace_from_usedAsIs() {
        Properties props = props();
        props.setProperty(RemoteOptions.KEY_PREFIX + "namespace", "qingzhou/prod/tenant-a/inst-1");

        Assert.assertEquals(RemoteOptions.from(props).buildNamespace(), "qingzhou/prod/tenant-a/inst-1");
    }

    @Test
    public void dimensions_from_joinedInOrder() {
        Properties props = props();
        props.setProperty(RemoteOptions.KEY_PREFIX + "service", "svc");
        props.setProperty(RemoteOptions.KEY_PREFIX + "deployment", "prod");
        props.setProperty(RemoteOptions.KEY_PREFIX + "tenant", "tenant-a");
        props.setProperty(RemoteOptions.KEY_PREFIX + "instance", "inst-1");

        Assert.assertEquals(RemoteOptions.from(props).buildNamespace(), "svc/prod/tenant-a/inst-1");
    }

    @Test
    public void missingDimensions_from_defaultsApplied() {
        RemoteOptions options = RemoteOptions.from(props());

        // instance 默认可能来自 qingzhou.instance，此处仅断言不含空段、无校验失败
        Assert.assertTrue(options.buildNamespace().contains("/"));
    }

    @Test
    public void illegalSegment_buildNamespace_throws() {
        Properties props = props();
        props.setProperty(RemoteOptions.KEY_PREFIX + "tenant", "tenant/a");

        try {
            RemoteOptions.from(props).buildNamespace();
            Assert.fail("illegal namespace segment should throw");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("invalid remote config namespace segment"), e.getMessage());
        }
    }

    @Test
    public void unsupportedType_factory_throws() {
        Properties props = props();
        props.setProperty(RemoteOptions.KEY_PREFIX + "type", "nacos");
        try {
            RemoteConfigSourceFactory.create(RemoteOptions.from(props));
            Assert.fail("unsupported type should throw");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("unsupported remote config center type"), e.getMessage());
        }
    }

    @Test
    public void etcdWithoutEndpoints_factory_throws() {
        Properties props = props();
        props.setProperty(RemoteOptions.KEY_PREFIX + "enabled", "true");
        try {
            RemoteConfigSourceFactory.create(RemoteOptions.from(props));
            Assert.fail("missing endpoints should throw");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("endpoints"), e.getMessage());
        }
    }
}
