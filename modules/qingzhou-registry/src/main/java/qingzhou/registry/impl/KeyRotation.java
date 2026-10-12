package qingzhou.registry.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 密钥轮换宽限。轮换存在一个 RTT 窗口：registry 切到新钥后，agent 要收到 refresh 响应才切换，
 * 期间 registry 用新钥加密的 invoke 必然解密失败（每 6 秒轮换一次 = 高频开窗）。
 * <p>
 * 解法：轮换后宽限期内 registry 仍用旧钥加密（agent 侧同步保留旧钥用于解密回退，
 * 见 agent 的 Heartbeat/ProcessHandler），invoke 不再因轮换必然失败。
 */
class KeyRotation {

    private final long graceMillis;
    // instanceId → 轮换前的旧钥
    private final Map<String, PreviousKey> previousKeys = new ConcurrentHashMap<>();

    KeyRotation(long graceMillis) {
        this.graceMillis = graceMillis;
    }

    /**
     * 记录轮换前的旧钥：宽限期内 invoke 继续用它加密
     */
    void onRotated(String instanceId, String oldKey) {
        previousKeys.put(instanceId, new PreviousKey(oldKey, System.currentTimeMillis()));
    }

    /**
     * 重新注册意味着密钥已由 agent 重新建立，registry 侧的旧钥宽限立即作废
     */
    void onReRegistered(String instanceId) {
        previousKeys.remove(instanceId);
    }

    /**
     * invoke 实际应使用的加密钥：宽限期内返回旧钥，其余返回实例当前钥
     */
    String getEffectiveKey(String instanceId, String newKey) {
        PreviousKey previous = previousKeys.get(instanceId);
        if (previous == null) return newKey;
        if (System.currentTimeMillis() - previous.rotatedAt > graceMillis) {
            // 两参 remove：宽限过期与新一轮换并发时，不误删新记录
            previousKeys.remove(instanceId, previous);
            return newKey;
        }
        return previous.key;
    }

    private static final class PreviousKey {
        final String key;
        final long rotatedAt;

        PreviousKey(String key, long rotatedAt) {
            this.key = key;
            this.rotatedAt = rotatedAt;
        }
    }
}
