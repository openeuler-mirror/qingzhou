package qingzhou.config.remote.etcd;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class JsonTest {
    @Test
    public void rangeResponse_parse_returnsKvsList() {
        Object root = Json.parse("{\"header\":{\"cluster_id\":1},\"kvs\":[{\"key\":\"aA==\",\"value\":\"Yg==\",\"create_revision\":1}],\"count\":1}");

        Assert.assertTrue(root instanceof Map);
        Object kvs = ((Map<?, ?>) root).get("kvs");
        Assert.assertTrue(kvs instanceof List);
        Map<?, ?> kv = (Map<?, ?>) ((List<?>) kvs).get(0);
        Assert.assertEquals(kv.get("key"), "aA==");
        Assert.assertEquals(kv.get("value"), "Yg==");
    }

    @Test
    public void escapedString_parse_decoded() {
        Object root = Json.parse("{\"token\":\"a\\\"b\\\\c\\n\"}");

        Assert.assertEquals(((Map<?, ?>) root).get("token"), "a\"b\\c\n");
    }

    @Test
    public void emptyKvs_parse_returnsEmptyList() {
        Object root = Json.parse("{\"kvs\":[],\"count\":0}");

        Assert.assertEquals(((List<?>) ((Map<?, ?>) root).get("kvs")).size(), 0);
    }

    @Test
    public void malformedJson_parse_throws() {
        try {
            Json.parse("{\"kvs\":[}");
            Assert.fail("malformed json should throw");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage() != null);
        }
    }
}
