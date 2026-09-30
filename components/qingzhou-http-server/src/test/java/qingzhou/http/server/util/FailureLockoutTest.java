package qingzhou.http.server.util;

import java.lang.reflect.Field;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FailureLockoutTest {
    @Test
    public void clear_unlocksAndResets() {
        FailureLockout throttle = new FailureLockout(2, 60_000, 100);
        throttle.recordFailure("ip1");
        Assert.assertFalse(throttle.isLocked("ip1"));

        throttle.recordFailure("ip1");
        Assert.assertTrue(throttle.isLocked("ip1")); // 达阈值锁定

        throttle.clear("ip1");

        Assert.assertFalse(throttle.isLocked("ip1"));
    }

    @Test
    public void windowExpired_resetsCount() throws Exception {
        FailureLockout throttle = new FailureLockout(2, 10, 100); // 10ms 窗口
        throttle.recordFailure("ip1");
        throttle.recordFailure("ip1");
        Assert.assertTrue(throttle.isLocked("ip1"));

        Thread.sleep(30); // 等待窗口过期
        Assert.assertFalse(throttle.isLocked("ip1")); // 过期即解锁
    }

    @Test
    public void capacityOverflow_evictsOldestEntries() throws Exception {
        FailureLockout throttle = new FailureLockout(5, 60_000, 100); // 水位 90
        for (int i = 0; i < 101; i++) {
            throttle.recordFailure(key(i));
            Thread.sleep(1); // 拉开 lastFailureAt，保证「最旧优先」断言的确定性
        }

        Assert.assertFalse(containsKey(throttle, key(0)), "oldest source must be evicted");
        Assert.assertTrue(containsKey(throttle, key(100)), "newest source must survive");
        Assert.assertEquals(countStates(throttle), 90); // 一次淘汰到低水位
    }

    // 用 IP 字面量构造 key：与真实来源一致且各不相同
    private static String key(int index) {
        return "10." + (index / 256 % 256) + "." + (index % 256) + ".1";
    }

    private static Map<?, ?> statesOf(FailureLockout throttle) throws Exception {
        Field field = FailureLockout.class.getDeclaredField("states");
        field.setAccessible(true);
        return (Map<?, ?>) field.get(throttle);
    }

    private static boolean containsKey(FailureLockout throttle, String key) throws Exception {
        return statesOf(throttle).containsKey(key);
    }

    private static int countStates(FailureLockout throttle) throws Exception {
        return statesOf(throttle).size();
    }
}
