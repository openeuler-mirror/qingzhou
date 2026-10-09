package qingzhou.ai.memory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MemoryLimitsTest {
    private static Map<String, String> config(String key, String value) {
        return Collections.singletonMap(key, value);
    }

    @Test
    public void defaults_validWithoutConfig() {
        MemoryLimits limits = new MemoryLimits(Collections.emptyMap());
        Assert.assertTrue(limits.maxBytes > 0);
        Assert.assertTrue(limits.hardMessages >= limits.softMessages);
        Assert.assertTrue(limits.keepMessages <= limits.softMessages);
    }

    @Test
    public void negativeValue_failsFast() {
        try {
            new MemoryLimits(config("conversation_hard_messages", "-5"));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("conversation_hard_messages"), e.getMessage());
        }
    }

    @Test
    public void zeroValue_failsFast() {
        try {
            new MemoryLimits(config("memory_max_conversations", "0"));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("memory_max_conversations"), e.getMessage());
        }
    }

    @Test
    public void nonNumeric_failsFast() {
        try {
            new MemoryLimits(config("message_max_chars", "abc"));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("message_max_chars"), e.getMessage());
        }
    }

    @Test
    public void intOverflow_failsFastInsteadOfTruncating() {
        try {
            new MemoryLimits(config("memory_max_conversations", "99999999999"));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("memory_max_conversations"), e.getMessage());
        }
    }

    @Test
    public void keepGreaterThanSoft_failsFast() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("conversation_keep_messages", "200");
            config.put("conversation_soft_messages", "80");
            new MemoryLimits(config);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("keep_messages must be <= soft"), e.getMessage());
        }
    }
}
