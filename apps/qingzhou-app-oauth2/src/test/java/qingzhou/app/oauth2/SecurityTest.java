package qingzhou.app.oauth2;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SecurityTest {

    @Test
    public void registeredRedirect_matchingRequest_passes() {
        Assert.assertNull(Security.validateRedirectUri("https://a.com/cb", "https://a.com/cb"));
    }

    @Test
    public void registeredRedirect_omittedRequest_passes() {
        Assert.assertNull(Security.validateRedirectUri(null, "https://a.com/cb"));
    }

    @Test
    public void registeredRedirect_otherRequest_rejected() { // 否则授权码会被投递到攻击者地址
        Assert.assertNotNull(Security.validateRedirectUri("https://evil.com", "https://a.com/cb"));
    }

    @Test
    public void unregisteredRedirect_rejected() {
        Assert.assertNotNull(Security.validateRedirectUri("https://a.com/cb", null));
    }

    @Test
    public void fileSchemeRedirect_rejected() {
        Assert.assertNotNull(Security.validateRedirectUri("file:///etc/passwd", "file:///etc/passwd"));
    }

    @Test
    public void crlfRedirect_rejected() {
        Assert.assertNotNull(Security.validateRedirectUri(null, "https://a.com/cb\r\nSet-Cookie: a=b"));
    }

    @Test
    public void scopeWithinClientScope_passes() {
        Assert.assertNull(Security.validateScope("read write", "read write admin"));
    }

    @Test
    public void scopeBeyondClientScope_rejected() { // 否则等于绕过授权范围限制
        Assert.assertNotNull(Security.validateScope("admin", "read write"));
    }

    @Test
    public void emptyScope_passes() {
        Assert.assertNull(Security.validateScope(null, "read"));
    }

    @Test
    public void registeredGrant_grantAllowed() {
        Assert.assertTrue(Security.grantAllowed(client("authorization_code,password"), "password"));
    }

    @Test
    public void unregisteredGrant_grantDenied() { // 客户端不得使用未登记的授予方式
        Assert.assertFalse(Security.grantAllowed(client("authorization_code"), "client_credentials"));
        Assert.assertFalse(Security.grantAllowed(client("authorization_code"), null));
    }

    @Test
    public void htmlChars_escaped() {
        Assert.assertEquals(Security.escapeHtml("<img src=x onerror='a'>"), "&lt;img src=x onerror=&#39;a&#39;&gt;");
    }

    private static Map<String, String> client(String grantTypes) {
        Map<String, String> client = new HashMap<>();
        client.put("grant_types", grantTypes);
        return client;
    }
}
