package qingzhou.config.impl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ConfigTest {
    @DataProvider
    public Object[][] configText() {
        return new Object[][]{
                {"a=1\nb=2\n", 2, "a", "1"},
                {"a=1\n\n\nb=2\n", 2, "b", "2"},// 空行忽略
                {"# comment\na=1\n#another\nb=2\n", 2, "b", "2"},// 注释忽略
                {"   a=1\n\u3000\u3000b=2\n", 2, "b", "2"},// 左侧普通空格与全角空格去除
                {"a=  1  \nb=2  \n", 2, "a", "1"},// 值两侧空白去除
                {"no-equals-line\n", 1, "no-equals-line", ""},// 无等号：整行作为 key
                {"=abc\n", 1, "=abc", ""},// 等号在行首：整行作为 key
                {"a=hello\\\nworld\n", 1, "a", "helloworld"},// 行尾反斜杠折行
                {"a=1\n\\\nb=2\n", 2, "a", "1"}// 单独反斜杠视为换行
        };
    }

    @Test(dataProvider = "configText")
    public void configText_parseConfig_producesExpectedEntry(String content, int size, String key, String value) throws Exception {
        Properties props = parse(content);

        Assert.assertEquals(props.size(), size);
        Assert.assertEquals(props.getProperty(key), value);
    }

    @Test
    public void qingzhouPrefix_init_distributeToNormalConfiguration() throws Exception {
        Map<String, Dictionary<String, Object>> updated = runInit("qingzhou-http-server.port=7900\n");

        Assert.assertEquals(updated.get("qingzhou-http-server").get("port"), "7900");
    }

    @Test
    public void appPrefix_init_distributeToFactoryConfiguration() throws Exception {
        Map<String, Dictionary<String, Object>> updated = runInit("app~redis~default.port=6379\n");

        Assert.assertEquals(updated.get("app~redis~default").get("port"), "6379");
    }

    @Test
    public void otherPrefix_init_ignored() throws Exception {
        Assert.assertTrue(runInit("other.key=value\n").isEmpty());
    }

    @Test
    public void samePidMultipleKeys_init_aggregatedToSameConfiguration() throws Exception {
        Map<String, Dictionary<String, Object>> updated = runInit(
                "qingzhou-http-server.port=7900\nqingzhou-http-server.host=0.0.0.0\n");

        Assert.assertEquals(updated.size(), 1);
        Assert.assertEquals(updated.get("qingzhou-http-server").get("port"), "7900");
        Assert.assertEquals(updated.get("qingzhou-http-server").get("host"), "0.0.0.0");
    }

    // ---------- 辅助 ----------

    private static Map<String, Dictionary<String, Object>> runInit(String localConfig) throws Exception {
        Map<String, Dictionary<String, Object>> updated = new HashMap<>();
        TestSupport.instance(localConfig);

        Config config = new Config();
        TestSupport.inject(config, "configAdmin", TestSupport.admin(updated));
        config.init();
        return updated;
    }

    private Properties parse(String content) throws Exception {
        Path file = Files.createTempFile("config-test", ".properties");
        try {
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            return Config.parseConfig(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
