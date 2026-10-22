package qingzhou.logger.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LogThrottleTest {
    private static final long WINDOW_MS = 30_000L;

    @Test
    public void sameMessageWithinWindow_onlyFirstPasses() {
        LogThrottle throttle = new LogThrottle();
        Assert.assertEquals(throttle.acquire('W', "msg", WINDOW_MS, 1000L), 0L);
        Assert.assertEquals(throttle.acquire('W', "msg", WINDOW_MS, 1001L), -1L);
        Assert.assertEquals(throttle.acquire('W', "msg", WINDOW_MS, 1002L), -1L);
    }

    @Test
    public void newWindow_reportsSuppressedCount() {
        LogThrottle throttle = new LogThrottle();
        throttle.acquire('W', "msg", WINDOW_MS, 1000L);
        throttle.acquire('W', "msg", WINDOW_MS, 1001L);
        throttle.acquire('W', "msg", WINDOW_MS, 1002L);
        Assert.assertEquals(throttle.acquire('W', "msg", WINDOW_MS, 1000L + WINDOW_MS), 2L);
    }

    @Test
    public void differentLevel_notSuppressed() {
        LogThrottle throttle = new LogThrottle();
        Assert.assertEquals(throttle.acquire('W', "msg", WINDOW_MS, 1000L), 0L);
        Assert.assertEquals(throttle.acquire('E', "msg", WINDOW_MS, 1001L), 0L);
    }

    @Test
    public void longNumber_normalized_shortNumberKept() {
        LogThrottle throttle = new LogThrottle();
        Assert.assertEquals(throttle.acquire('E', "timeout at 1700000000000", WINDOW_MS, 1000L), 0L);
        Assert.assertEquals(throttle.acquire('E', "timeout at 1800000000000", WINDOW_MS, 1001L), -1L);
        Assert.assertEquals(throttle.acquire('E', "response code 500", WINDOW_MS, 1002L), 0L);
        Assert.assertEquals(throttle.acquire('E', "response code 404", WINDOW_MS, 1003L), 0L);
    }
}
