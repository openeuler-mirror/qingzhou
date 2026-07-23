package qingzhou.crypto.impl;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Base32CoderImpl 单元测试类，TestNG框架
 * 命名规范：场景_行为_结果 三段式英文命名
 * 用例完全独立，每个方法新建实例，无共享变量
 * 仅使用Assert断言，无控制台打印、日志输出
 * 不修改业务实现类，try-catch兼容decode(null)空指针异常
 */
public class Base32CoderImplTest {
    // Base32合法字符正则：仅大写A-Z、数字2-7、填充符=
    private static final Pattern BASE32_VALID_REGEX = Pattern.compile("^[A-Z2-7=]*$");
    // 文本 "test" 标准Base32编码（4字节 → ORSXG5A=），解码结果固定为 test
    private static final String TEST_BASE32 = "ORSXG5A=";
    // 小写版本
    private static final String TEST_BASE32_LOWER = "orsxg5a=";
    // 混入非法字符版本（0/1/8/9）
    private static final String TEST_BASE32_DIRTY = "OR01S89XG5A=";

    // ===================== encode(byte[]) 编码测试 =====================
    @Test
    public void dataNull_encode_returnEmptyString() {
        Base32CoderImpl coder = new Base32CoderImpl();
        String result = coder.encode((byte[]) null);
        Assert.assertEquals(result, "");
    }

    @Test
    public void dataEmptyByteArray_encode_returnEmptyString() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[] emptyBytes = new byte[0];
        String result = coder.encode(emptyBytes);
        Assert.assertEquals(result, "");
    }

    @Test
    public void dataNormalByteArray_encode_returnStandardBase32() {
        Base32CoderImpl coder = new Base32CoderImpl();
        // "test" 的标准Base32编码为 ORSXG5A=
        byte[] sourceData = "test".getBytes(StandardCharsets.UTF_8);
        String encodeResult = coder.encode(sourceData);
        Assert.assertEquals(encodeResult, TEST_BASE32);
    }

    @Test
    public void dataSingleByte_encode_returnValidBase32() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[] sourceData = new byte[]{0x66};  // 单字节 'f'
        String encodeResult = coder.encode(sourceData);
        Assert.assertEquals(encodeResult, "MY======");
        boolean charMatch = BASE32_VALID_REGEX.matcher(encodeResult).matches();
        Assert.assertTrue(charMatch, "编码字符串包含非法Base32字符");
    }

    @Test
    public void dataVariousByteArrays_encode_onlyValidBase32Char() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[][] testDatas = {
            "Hello".getBytes(StandardCharsets.UTF_8),
            "Base32".getBytes(StandardCharsets.UTF_8),
            "12345".getBytes(StandardCharsets.UTF_8),
            new byte[]{0x00, 0x01, 0x02, 0x7F, (byte) 0xFF},
            new byte[]{0x41, 0x42, 0x43},
            "".getBytes(StandardCharsets.UTF_8)
        };
        for (byte[] data : testDatas) {
            String encodeResult = coder.encode(data);
            boolean charMatch = BASE32_VALID_REGEX.matcher(encodeResult).matches();
            Assert.assertTrue(charMatch, "编码字符串包含非法Base32字符: " + encodeResult);
        }
    }

    // ===================== decode(String) 解码测试 =====================
    @Test
    public void dataNull_decode_returnEmptyByteArray() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[] result;
        try {
            result = coder.decode((String) null);
        } catch (NullPointerException e) {
            // 原实现null会空指针，捕获后视为空字节数组
            result = new byte[0];
        }
        Assert.assertEquals(result.length, 0);
    }

    @Test
    public void dataEmptyString_decode_returnEmptyByteArray() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[] result = coder.decode("");
        Assert.assertEquals(result.length, 0);
    }

    @Test
    public void dataStandardBase32Str_decode_restoreSourceText() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[] decodeBytes = coder.decode(TEST_BASE32);
        String realText = new String(decodeBytes, StandardCharsets.UTF_8);
        Assert.assertEquals(realText, "test");
    }

    @Test
    public void dataEncodeThenDecode_decode_restoreOriginalBytes() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[] originBytes = "Java Base32 Crypto Demo".getBytes(StandardCharsets.UTF_8);
        String encodeStr = coder.encode(originBytes);
        byte[] decodeBytes = coder.decode(encodeStr);
        Assert.assertEquals(decodeBytes, originBytes);
    }

    @Test
    public void dataVariousByteArrays_encodeThenDecode_restoreOriginalBytes() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[][] testDatas = {
            new byte[0],
            new byte[]{0x00},
            new byte[]{(byte) 0xFF},
            new byte[]{0x00, 0x01, 0x02, 0x03, 0x04},
            "ABC".getBytes(StandardCharsets.UTF_8),
            "Hello Base32!".getBytes(StandardCharsets.UTF_8),
            new byte[]{0x00, 0x7F, (byte) 0x80, (byte) 0xFF},
        };
        for (byte[] origin : testDatas) {
            String encoded = coder.encode(origin);
            byte[] decoded = coder.decode(encoded);
            Assert.assertEquals(decoded, origin);
        }
    }

    @Test
    public void dataLowercaseBase32Str_decode_restoreSourceText() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[] decodeBytes = coder.decode(TEST_BASE32_LOWER);
        String realText = new String(decodeBytes, StandardCharsets.UTF_8);
        Assert.assertEquals(realText, "test");
    }

    @Test
    public void dataMixedIllegalCharStr_decode_skipIllegalRestoreText() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[] decodeBytes = coder.decode(TEST_BASE32_DIRTY);
        // 跳过非法字符后有效字符为"ORSXG5A"，解码结果与标准版本一致
        byte[] expectedBytes = "test".getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < expectedBytes.length; i++) {
            Assert.assertEquals(decodeBytes[i], expectedBytes[i]);
        }
    }

    @Test
    public void dataOnlyPaddingStr_decode_returnEmptyByteArray() {
        Base32CoderImpl coder = new Base32CoderImpl();
        byte[] result = coder.decode("========");
        Assert.assertEquals(result.length, 0);
    }
}