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
    /** 依次覆盖：空行、注释、左侧空白（含全角）、值两侧空白、无等号、等号在行首、折行、单独反斜杠。 */
    @DataProvider
    public Object[][] configText() {
        return new Object[][]{
                {"a=1\nb=2\n", 2, "a", "1"},
                {"a=1\n\n\nb=2\n", 2, "b", "2"},
                {"# comment\na=1\n#another\nb=2\n", 2, "b", "2"},
                {"   a=1\n\u3000\u3000b=2\n", 2, "b", "2"},
                {"a=  1  \nb=2  \n", 2, "a", "1"},
                {"no-equals-line\n", 1, "no-equals-line", ""},
                {"=abc\n", 1, "=abc", ""},
                {"a=hello\\\nworld\n", 1, "a", "helloworld"},
                {"a=1\n\\\nb=2\n", 2, "a", "1"}
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
