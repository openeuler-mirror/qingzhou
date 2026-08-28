package qingzhou.config.impl;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConfigTest {
    // ===================== parseConfig 配置文件解析测试 =====================

    @Test
    public void normalKeyValue_parseConfig_parseCorrectly() throws Exception {
        Properties props = parse("a=1\nb=2\n");

        Assert.assertEquals(props.getProperty("a"), "1");
        Assert.assertEquals(props.getProperty("b"), "2");
    }

    @Test
    public void blankLines_parseConfig_ignored() throws Exception {
        Properties props = parse("a=1\n\n\nb=2\n");

        Assert.assertEquals(props.size(), 2);
        Assert.assertEquals(props.getProperty("a"), "1");
        Assert.assertEquals(props.getProperty("b"), "2");
    }

    @Test
    public void commentLines_parseConfig_ignored() throws Exception {
        Properties props = parse("# comment\na=1\n#another\nb=2\n");

        Assert.assertEquals(props.size(), 2);
        Assert.assertEquals(props.getProperty("a"), "1");
        Assert.assertEquals(props.getProperty("b"), "2");
    }

    @Test
    public void leadingWhitespace_parseConfig_removed() throws Exception {
        // 左侧普通空格 + 全角空格 \u3000
        Properties props = parse("   a=1\n\u3000\u3000b=2\n");

        Assert.assertEquals(props.getProperty("a"), "1");
        Assert.assertEquals(props.getProperty("b"), "2");
    }

    @Test
    public void valueWhitespace_parseConfig_trimmed() throws Exception {
        Properties props = parse("a=  1  \nb=2  \n");

        Assert.assertEquals(props.getProperty("a"), "1");
        Assert.assertEquals(props.getProperty("b"), "2");
    }

    @Test
    public void lineWithoutEquals_parseConfig_keyWithEmptyValue() throws Exception {
        Properties props = parse("no-equals-line\n");

        Assert.assertTrue(props.containsKey("no-equals-line"));
        Assert.assertEquals(props.getProperty("no-equals-line"), "");
    }

    @Test
    public void equalsAtLineStart_parseConfig_wholeLineAsKey() throws Exception {
        // = 在行首，i==0 走 else 分支，整行（含 =）作为 key，值为空
        Properties props = parse("=abc\n");

        Assert.assertTrue(props.containsKey("=abc"));
        Assert.assertEquals(props.getProperty("=abc"), "");
    }

    @Test
    public void trailingBackslash_parseConfig_joinsNextLine() throws Exception {
        Properties props = parse("a=hello\\\nworld\n");

        Assert.assertEquals(props.getProperty("a"), "helloworld");
    }

    @Test
    public void singleBackslashLine_parseConfig_ignored() throws Exception {
        Properties props = parse("a=1\n\\\nb=2\n");

        Assert.assertEquals(props.size(), 2);
        Assert.assertEquals(props.getProperty("a"), "1");
        Assert.assertEquals(props.getProperty("b"), "2");
    }

    // ===================== init 配置分发测试 =====================

    @Test
    public void qingzhouPrefix_init_distributeToNormalConfiguration() throws Exception {
        Map<String, Dictionary<String, Object>> updated = new HashMap<>();
        ConfigurationAdmin admin = newConfigAdminStub(updated, false);

        writeInstanceProperties("qingzhou-http-server.port=7900\n");

        runInit(admin);

        Assert.assertTrue(updated.containsKey("qingzhou-http-server"));
        Dictionary<String, Object> dict = updated.get("qingzhou-http-server");
        Assert.assertEquals(dict.get("port"), "7900");
    }

    @Test
    public void appPrefix_init_distributeToFactoryConfiguration() throws Exception {
        Map<String, Dictionary<String, Object>> updated = new HashMap<>();
        ConfigurationAdmin admin = newConfigAdminStub(updated, true);

        writeInstanceProperties("app~redis~default.port=6379\n");

        runInit(admin);

        Assert.assertTrue(updated.containsKey("app~redis~default"));
        Dictionary<String, Object> dict = updated.get("app~redis~default");
        Assert.assertEquals(dict.get("port"), "6379");
    }

    @Test
    public void otherPrefix_init_ignored() throws Exception {
        Map<String, Dictionary<String, Object>> updated = new HashMap<>();
        ConfigurationAdmin admin = newConfigAdminStub(updated, false);

        writeInstanceProperties("other.key=value\n");

        runInit(admin);

        Assert.assertTrue(updated.isEmpty());
    }

    @Test
    public void samePidMultipleKeys_init_aggregatedToSameConfiguration() throws Exception {
        Map<String, Dictionary<String, Object>> updated = new HashMap<>();
        ConfigurationAdmin admin = newConfigAdminStub(updated, false);

        writeInstanceProperties("qingzhou-http-server.port=7900\nqingzhou-http-server.host=0.0.0.0\n");

        runInit(admin);

        Assert.assertEquals(updated.size(), 1);
        Dictionary<String, Object> dict = updated.get("qingzhou-http-server");
        Assert.assertEquals(dict.get("port"), "7900");
        Assert.assertEquals(dict.get("host"), "0.0.0.0");
    }

    // ===================== 辅助方法 =====================

    private Properties parse(String content) throws Exception {
        Path file = Files.createTempFile("config-test", ".properties");
        try {
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            return Config.parseConfig(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private void writeInstanceProperties(String content) throws Exception {
        Path instanceDir = Files.createTempDirectory("qingzhou-instance");
        Path confDir = Files.createDirectories(instanceDir.resolve("conf"));
        Files.write(confDir.resolve("qingzhou.properties"), content.getBytes(StandardCharsets.UTF_8));
        System.setProperty("qingzhou.instance", instanceDir.toString());
    }

    private void runInit(ConfigurationAdmin admin) throws Exception {
        Config config = new Config();
        setField(config, "configAdmin", admin);
        config.init();
    }

    private static void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 桩 ConfigurationAdmin：记录 getConfiguration / getFactoryConfiguration 返回的
     * Configuration 对应的 pid（作为 key）以及 update 时的属性字典。
     * useFactory 为 true 时走工厂配置方法，否则走普通配置方法。
     */
    private ConfigurationAdmin newConfigAdminStub(Map<String, Dictionary<String, Object>> updated, boolean useFactory) {
        return (ConfigurationAdmin) Proxy.newProxyInstance(
                ConfigTest.class.getClassLoader(),
                new Class<?>[]{ConfigurationAdmin.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getConfiguration".equals(name)) {
                        return newRecordingConfiguration((String) args[0], updated);
                    }
                    if ("getFactoryConfiguration".equals(name)) {
                        String factoryPid = (String) args[0];
                        String instanceName = (String) args[1];
                        return newRecordingConfiguration(factoryPid + "~" + instanceName, updated);
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private Configuration newRecordingConfiguration(String pid, Map<String, Dictionary<String, Object>> updated) {
        return (Configuration) Proxy.newProxyInstance(
                ConfigTest.class.getClassLoader(),
                new Class<?>[]{Configuration.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("update".equals(name)) {
                        updated.put(pid, (Dictionary<String, Object>) args[0]);
                        return null;
                    }
                    if ("getPid".equals(name)) return pid;
                    if ("getFactoryPid".equals(name)) return null;
                    if ("getBundleLocation".equals(name)) return null;
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    return null;
                });
    }
}
