package qingzhou.http.server;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ContextPathTest {
    @Test
    public void normalize_validConfigurations_returnsNormalizedPath() {
        Assert.assertEquals(ContextPath.normalize("/"), "");
        Assert.assertEquals(ContextPath.normalize("/qingzhou"), "/qingzhou");
        Assert.assertEquals(ContextPath.normalize("/api/"), "/api");
        Assert.assertEquals(ContextPath.normalize("/api/v1/"), "/api/v1");
    }

    @Test
    public void normalize_illegalConfigurations_failsFast() {
        for (String illegal : new String[]{"api/v1", "", "/a//b", "/a/../b", "/a?b", "/a#b", "/a%2fb", "/中文", null,
                "/" + new String(new char[ContextPath.MAX_CHARS]).replace('\0', 'a')}) {
            try {
                ContextPath.normalize(illegal);
                Assert.fail("accepted illegal context_path: " + illegal);
            } catch (IllegalArgumentException expected) {
                // 配置入口非法即抛：激活失败而非全站 404
            }
        }
    }

    @Test
    public void normalizedOutput_alwaysPassesBoundaryCheck() {
        for (String configured : new String[]{"/", "/qingzhou", "/api/", "/api/v1", "/a-b_c.d~e"}) {
            Assert.assertTrue(ContextPath.isNormalized(ContextPath.normalize(configured)));
        }
    }

    @Test
    public void isNormalized_rejectsUntrustedValues() {
        for (String illegal : new String[]{"api/v1", "/a/", "/a//b", "/a/../b", "/a?b", "/中文", null,
                "/" + new String(new char[ContextPath.MAX_CHARS]).replace('\0', 'a')}) {
            Assert.assertFalse(ContextPath.isNormalized(illegal), "accepted: " + illegal);
        }
        Assert.assertTrue(ContextPath.isNormalized(""));
        Assert.assertTrue(ContextPath.isNormalized("/api/v1"));
    }
}
