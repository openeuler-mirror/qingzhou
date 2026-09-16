package qingzhou.app.oauth2;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ThrottleTest {

    @Test
    public void failuresBelowThreshold_notLocked() {
        Throttle throttle = new Throttle();
        for (int i = 0; i < 4; i++) {
            throttle.recordFailure("ip");
        }
        Assert.assertFalse(throttle.isLocked("ip"));
    }

    @Test
    public void failuresReachThreshold_locked() { // 阈值后锁定，防止口令爆破
        Throttle throttle = new Throttle();
        for (int i = 0; i < 5; i++) {
            throttle.recordFailure("ip");
        }
        Assert.assertTrue(throttle.isLocked("ip"));
        Assert.assertFalse(throttle.isLocked("other")); // 各来源互不影响
    }

    @Test
    public void clearedFailures_notLocked() {
        Throttle throttle = new Throttle();
        for (int i = 0; i < 5; i++) {
            throttle.recordFailure("ip");
        }
        throttle.clear("ip");
        Assert.assertFalse(throttle.isLocked("ip"));
    }
}
