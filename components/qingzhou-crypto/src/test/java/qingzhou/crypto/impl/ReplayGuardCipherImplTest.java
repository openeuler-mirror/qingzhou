package qingzhou.crypto.impl;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.Test;

import qingzhou.crypto.ReplayGuardCipher;
import qingzhou.crypto.ReplayGuardCipher.ReplayGuardException.Reason;

public class ReplayGuardCipherImplTest {
    // 每个用例独立密钥与 guard，避免共享去重状态互相干扰
    private ReplayGuardCipher newGuard() {
        try {
            CryptoImpl crypto = new CryptoImpl();
            return crypto.getReplayGuardCipher(crypto.getCipher(crypto.generateKey()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void encryptThenDecrypt_roundTrip() throws Exception {
        ReplayGuardCipher guard = newGuard();
        byte[] packet = guard.encrypt("hello".getBytes(StandardCharsets.UTF_8), null);
        Assert.assertEquals(new String(guard.decrypt(packet, null), StandardCharsets.UTF_8), "hello");
    }

    @Test
    public void secondDecrypt_samePacket_throwsReplay() throws Exception {
        ReplayGuardCipher guard = newGuard();
        byte[] packet = guard.encrypt("once".getBytes(StandardCharsets.UTF_8), null);
        Assert.assertEquals(new String(guard.decrypt(packet, null), StandardCharsets.UTF_8), "once");
        try {
            guard.decrypt(packet, null);
            Assert.fail();
        } catch (ReplayGuardCipher.ReplayGuardException e) {
            Assert.assertEquals(e.getReason(), Reason.REPLAY);
        }
    }

    @Test
    public void tamperedTimestamp_throwsOutOfWindow() throws Exception {
        ReplayGuardCipher guard = newGuard();
        byte[] packet = guard.encrypt("fresh".getBytes(StandardCharsets.UTF_8), null);
        ByteBuffer.wrap(packet, 2, 8).putLong(System.currentTimeMillis() - 10 * 60 * 1000); // 改写头中时间戳
        try {
            guard.decrypt(packet, null);
            Assert.fail();
        } catch (ReplayGuardCipher.ReplayGuardException e) {
            Assert.assertEquals(e.getReason(), Reason.TIMESTAMP_OUT_OF_WINDOW);
        }
    }

    @Test
    public void wrongContext_decryptFails_andDoesNotBurnNonce() throws Exception {
        ReplayGuardCipher guard = newGuard();
        byte[] packet = guard.encrypt("payload".getBytes(StandardCharsets.UTF_8), "channel-A".getBytes(StandardCharsets.UTF_8));
        try {
            guard.decrypt(packet, "channel-B".getBytes(StandardCharsets.UTF_8));
            Assert.fail();
        } catch (ReplayGuardCipher.ReplayGuardException e) {
            Assert.fail("AAD 失配应表现为解密失败而非重放", e);
        } catch (Exception ignored) {
            // GCM 认证失败
        }
        // 关键断言：失败的尝试不得烧掉 nonce，正确上下文仍可解密
        Assert.assertEquals(new String(guard.decrypt(packet, "channel-A".getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8), "payload");
    }

    @Test
    public void concurrentDuplicateDecrypt_exactlyOneSuccess() throws Exception {
        final ReplayGuardCipher guard = newGuard();
        final byte[] packet = guard.encrypt("dup".getBytes(StandardCharsets.UTF_8), null);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger success = new AtomicInteger();
        Runnable task = () -> {
            try {
                start.await();
                guard.decrypt(packet, null);
                success.incrementAndGet();
            } catch (Throwable ignored) {
                // 失败方按重放被拒
            }
        };
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        start.countDown();
        t1.join();
        t2.join();
        Assert.assertEquals(success.get(), 1);
    }

    @Test
    public void garbageBytes_throwsInvalidPacket() {
        ReplayGuardCipher guard = newGuard();
        try {
            guard.decrypt("not a packet".getBytes(StandardCharsets.UTF_8), null);
            Assert.fail();
        } catch (ReplayGuardCipher.ReplayGuardException e) {
            Assert.assertEquals(e.getReason(), Reason.INVALID_PACKET);
        } catch (Exception e) {
            Assert.fail("非本协议报文应抛 ReplayGuardException", e);
        }
    }
}
