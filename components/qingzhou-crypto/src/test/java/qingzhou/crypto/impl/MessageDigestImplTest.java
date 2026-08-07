package qingzhou.crypto.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MessageDigestImplTest {

    // Base16CoderImpl 输出大写十六进制，标准哈希值需用大写
    private static final String MD5_ABC = "900150983CD24FB0D6963F7D28E17F72";
    private static final String MD5_EMPTY = "D41D8CD98F00B204E9800998ECF8427E";
    private static final String SHA256_ABC = "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD";
    private static final String SHA256_EMPTY = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";

    // 每个用例独立创建实例，避免共享状态
    private MessageDigestImpl newDigest() {
        return new MessageDigestImpl(new Base16CoderImpl());
    }

    // ===================== md5(String) =====================
    // null 视为空串，与 md5(byte[]) 的既有 null 容忍行为保持一致
    @Test
    public void nullInput_md5_returnNonEmptyHash() {
        String hash = newDigest().md5((String) null);
        Assert.assertNotNull(hash);
        Assert.assertFalse(hash.trim().isEmpty());
        Assert.assertEquals(hash, MD5_EMPTY);
    }

    @Test
    public void emptyString_md5_returnNonEmptyHash() {
        String hash = newDigest().md5("");
        Assert.assertNotNull(hash);
        Assert.assertFalse(hash.trim().isEmpty());
        Assert.assertEquals(hash, MD5_EMPTY);
    }

    @Test
    public void normalString_md5_returnCorrect32Hex() {
        String hash = newDigest().md5("abc");
        Assert.assertEquals(hash, MD5_ABC);
        Assert.assertTrue(hash.matches("[0-9A-F]{32}"));
    }

    @Test
    public void sameInput_md5_returnSameOutput() {
        MessageDigestImpl digest = newDigest();
        Assert.assertEquals(digest.md5("qingzhou"), digest.md5("qingzhou"));
    }

    // ===================== sha256(String) =====================

    @Test
    public void nullInput_sha256_returnNonEmptyHash() {
        String hash = newDigest().sha256((String) null);
        Assert.assertNotNull(hash);
        Assert.assertFalse(hash.trim().isEmpty());
        Assert.assertEquals(hash, SHA256_EMPTY);
    }

    @Test
    public void emptyString_sha256_returnNonEmptyHash() {
        String hash = newDigest().sha256("");
        Assert.assertNotNull(hash);
        Assert.assertFalse(hash.trim().isEmpty());
        Assert.assertEquals(hash, SHA256_EMPTY);
    }

    @Test
    public void normalString_sha256_returnCorrect64Hex() {
        String hash = newDigest().sha256("abc");
        Assert.assertEquals(hash, SHA256_ABC);
        Assert.assertTrue(hash.matches("[0-9A-F]{64}"));
    }

    @Test
    public void sameInput_sha256_returnSameOutput() {
        MessageDigestImpl digest = newDigest();
        Assert.assertEquals(digest.sha256("qingzhou"), digest.sha256("qingzhou"));
    }

    // ===================== digest(text, algorithm, saltLength, iterations) =====================
    @Test
    public void differentAlgorithms_digest_returnDifferentHash() {
        MessageDigestImpl digest = newDigest();
        String md5 = digest.digest("pwd", "MD5", 0, 1);
        String sha256 = digest.digest("pwd", "SHA-256", 0, 1);
        Assert.assertNotEquals(md5.split("\\$")[3], sha256.split("\\$")[3]);
        Assert.assertEquals(md5.split("\\$")[3].length(), 32);
        Assert.assertEquals(sha256.split("\\$")[3].length(), 64);
    }

    @Test
    public void differentSaltLength_digest_changeSaltSegment() {
        MessageDigestImpl digest = newDigest();
        String noSalt = digest.digest("pwd", "SHA-256", 0, 1);
        String salt8 = digest.digest("pwd", "SHA-256", 8, 1);
        String salt16 = digest.digest("pwd", "SHA-256", 16, 1);
        Assert.assertEquals(noSalt.split("\\$")[1].length(), 0);
        Assert.assertEquals(salt8.split("\\$")[1].length(), 16);
        Assert.assertEquals(salt16.split("\\$")[1].length(), 32);
        Assert.assertNotEquals(salt8, salt16);
    }

    @Test
    public void differentIterations_digest_returnDifferentHash() {
        MessageDigestImpl digest = newDigest();
        String iter1 = digest.digest("pwd", "SHA-256", 0, 1);
        String iter2 = digest.digest("pwd", "SHA-256", 0, 2);
        String iter3 = digest.digest("pwd", "SHA-256", 0, 3);
        Assert.assertNotEquals(iter1.split("\\$")[3], iter2.split("\\$")[3]);
        Assert.assertNotEquals(iter2.split("\\$")[3], iter3.split("\\$")[3]);
        Assert.assertNotEquals(iter1.split("\\$")[3], iter3.split("\\$")[3]);
    }

    @Test
    public void digest_resultFormat_containsAlgorithmSaltIterationsHash() {
        String result = newDigest().digest("pwd", "SHA-256", 8, 3);
        String[] parts = result.split("\\$");
        Assert.assertEquals(parts.length, 4);
        Assert.assertEquals(parts[0], "SHA-256");
        Assert.assertEquals(parts[1].length(), 16); // 8 字节盐 -> 16 位 hex
        Assert.assertEquals(parts[2], "3");
        Assert.assertTrue(parts[3].matches("[0-9A-F]{64}"));
    }

    // ===================== matches(text, msgDigest) =====================
    @Test
    public void correctPassword_matches_returnTrue() {
        MessageDigestImpl digest = newDigest();
        String result = digest.digest("pwd", "SHA-256", 8, 2);
        Assert.assertTrue(digest.matches("pwd", result));
    }

    @Test
    public void wrongPassword_matches_returnFalse() {
        MessageDigestImpl digest = newDigest();
        String result = digest.digest("pwd", "SHA-256", 8, 2);
        Assert.assertFalse(digest.matches("wrong", result));
    }

    @Test
    public void nullInput_matches_returnFalse() {
        MessageDigestImpl digest = newDigest();
        String result = digest.digest("pwd", "SHA-256", 8, 2);
        Assert.assertFalse(digest.matches(null, result));
        Assert.assertFalse(digest.matches("pwd", null));
    }

    @Test
    public void differentFormats_matches_parseCorrectly() {
        MessageDigestImpl digest = newDigest();
        String[] formats = {
                digest.digest("pwd", "MD5", 0, 1),     // 空盐格式 MD5$$1$...
                digest.digest("pwd", "MD5", 8, 5),     // 8 字节盐、迭代 5 次
                digest.digest("pwd", "SHA-256", 0, 1), // 空盐格式 SHA-256$$1$...
                digest.digest("pwd", "SHA-256", 16, 3), // 16 字节盐、迭代 3 次
        };
        for (String format : formats) {
            Assert.assertTrue(digest.matches("pwd", format), "无法解析格式: " + format);
        }
    }
}
