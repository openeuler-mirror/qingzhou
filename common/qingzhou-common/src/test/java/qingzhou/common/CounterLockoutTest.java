package qingzhou.common;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CounterLockoutTest {
    @Test
    public void clear_unlocksAndResets() {
        CounterLockout throttle = new CounterLockout(2, 60_000, 100);
        throttle.record("ip1");
        Assert.assertFalse(throttle.isLocked("ip1"));

        throttle.record("ip1");
        Assert.assertTrue(throttle.isLocked("ip1")); // 达阈值锁定

        throttle.clear("ip1");

        Assert.assertFalse(throttle.isLocked("ip1"));
    }

    @Test
    public void windowExpired_resetsCount() throws Exception {
        CounterLockout throttle = new CounterLockout(2, 10, 100); // 10ms 窗口
        throttle.record("ip1");
        throttle.record("ip1");
        Assert.assertTrue(throttle.isLocked("ip1"));

        Thread.sleep(30); // 等待窗口过期
        Assert.assertFalse(throttle.isLocked("ip1")); // 过期即解锁
    }
}
