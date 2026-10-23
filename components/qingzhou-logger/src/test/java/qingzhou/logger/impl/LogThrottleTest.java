package qingzhou.logger.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicLong;

public class LogThrottleTest {
    private static final long WINDOW_MS = 30_000L;

    private static LogThrottle throttleWith(AtomicLong now) {
        return new LogThrottle(now::get);
    }

    @Test
    public void sameMessageWithinWindow_onlyFirstPasses() {
        AtomicLong now = new AtomicLong(1000L);
        LogThrottle throttle = throttleWith(now);
        Assert.assertEquals(throttle.acquire('W', "msg"), 0L);
        now.set(1001L);
        Assert.assertEquals(throttle.acquire('W', "msg"), -1L);
        now.set(1002L);
        Assert.assertEquals(throttle.acquire('W', "msg"), -1L);
    }

    @Test
    public void newWindow_reportsSuppressedCount() {
        AtomicLong now = new AtomicLong(1000L);
        LogThrottle throttle = throttleWith(now);
        throttle.acquire('W', "msg");
        now.set(1001L);
        throttle.acquire('W', "msg");
        now.set(1002L);
        throttle.acquire('W', "msg");
        now.set(1000L + WINDOW_MS);
        Assert.assertEquals(throttle.acquire('W', "msg"), 2L);
    }

    @Test
    public void differentLevel_notSuppressed() {
        AtomicLong now = new AtomicLong(1000L);
        LogThrottle throttle = throttleWith(now);
        Assert.assertEquals(throttle.acquire('W', "msg"), 0L);
        now.set(1001L);
        Assert.assertEquals(throttle.acquire('E', "msg"), 0L);
    }

    @Test
    public void longNumber_normalized_shortNumberKept() {
        AtomicLong now = new AtomicLong(1000L);
        LogThrottle throttle = throttleWith(now);
        Assert.assertEquals(throttle.acquire('E', "timeout at 1700000000000"), 0L);
        now.set(1001L);
        Assert.assertEquals(throttle.acquire('E', "timeout at 1800000000000"), -1L);
        now.set(1002L);
        Assert.assertEquals(throttle.acquire('E', "response code 500"), 0L);
        now.set(1003L);
        Assert.assertEquals(throttle.acquire('E', "response code 404"), 0L);
    }
}
