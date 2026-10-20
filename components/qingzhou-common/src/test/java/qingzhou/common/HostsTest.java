package qingzhou.common;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HostsTest {
    @Test
    public void isLoopback_loopbackForms_accepted() {
        String[] hosts = {"localhost", "LOCALHOST", "127.0.0.1", "127.0.0.2", "127.255.255.255", "::1", "[::1]"};
        for (String host : hosts) {
            Assert.assertTrue(Hosts.isLoopback(host), host);
        }
    }

    @Test
    public void isLoopback_remoteOrInvalid_rejected() {
        String[] hosts = {null, "", "192.168.1.1", "10.0.0.1", "0.0.0.0", "127.0.0.1.example.com", "127.0.0.256"};
        for (String host : hosts) {
            Assert.assertFalse(Hosts.isLoopback(host), String.valueOf(host));
        }
    }
}
