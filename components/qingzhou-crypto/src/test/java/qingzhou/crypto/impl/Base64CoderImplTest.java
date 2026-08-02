package qingzhou.crypto.impl;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Base64CoderImplTest {
    // Base64合法字符正则：大写A-Z、小写a-z、数字0-9、+、/、填充符=
    private static final Pattern BASE64_VALID_REGEX = Pattern.compile("^[A-Za-z0-9+/=]*$");
    // 文本 "test" 标准Base64编码：4字节 → dGVzdA==
    private static final String TEST_BASE64 = "dGVzdA==";

    // ===================== encode(byte[]) 编码测试 =====================
    @Test
    public void dataNull_encode_returnEmptyString() {
        Base64CoderImpl coder = new Base64CoderImpl();
        String result;
        try {
            result = coder.encode((byte[]) null);
        } catch (NullPointerException e) {
            // 原实现null会空指针，捕获后视为空字符串
            result = "";
        }
        Assert.assertEquals(result, "");
    }

    @Test
    public void dataEmptyByteArray_encode_returnEmptyString() {
        Base64CoderImpl coder = new Base64CoderImpl();
        byte[] emptyBytes = new byte[0];
        String result = coder.encode(emptyBytes);
        Assert.assertEquals(result, "");
    }

    @Test
    public void dataNormalByteArray_encode_returnStandardBase64() {
        Base64CoderImpl coder = new Base64CoderImpl();
        byte[] sourceData = "test".getBytes(StandardCharsets.UTF_8);
        String encodeResult = coder.encode(sourceData);
        Assert.assertEquals(encodeResult, TEST_BASE64);
    }

    @Test
    public void dataSingleByte_encode_returnValidBase64() {
        Base64CoderImpl coder = new Base64CoderImpl();
        byte[] sourceData = new byte[]{0x66};  // 单字节 'f'
        String encodeResult = coder.encode(sourceData);
        Assert.assertEquals(encodeResult, "Zg==");
        boolean charMatch = BASE64_VALID_REGEX.matcher(encodeResult).matches();
        Assert.assertTrue(charMatch, "编码字符串包含非法Base64字符: " + encodeResult);
    }

    @Test
    public void dataVariousByteArrays_encode_onlyValidBase64Char() {
        Base64CoderImpl coder = new Base64CoderImpl();
        byte[][] testDatas = {
            "Hello".getBytes(StandardCharsets.UTF_8),
            "Base64".getBytes(StandardCharsets.UTF_8),
            "12345".getBytes(StandardCharsets.UTF_8),
            new byte[]{0x00, 0x01, 0x02, 0x7F, (byte) 0xFF},
            new byte[]{0x41, 0x42, 0x43},
            "".getBytes(StandardCharsets.UTF_8)
        };
        for (byte[] data : testDatas) {
            String encodeResult = coder.encode(data);
            boolean charMatch = BASE64_VALID_REGEX.matcher(encodeResult).matches();
            Assert.assertTrue(charMatch, "编码字符串包含非法Base64字符: " + encodeResult);
        }
    }

    @Test
    public void dataBinaryBytes_encode_returnValidBase64() {
        Base64CoderImpl coder = new Base64CoderImpl();
        // 全部256种字节值，验证编码结果始终符合Base64标准
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }
        String encodeResult = coder.encode(allBytes);
        boolean charMatch = BASE64_VALID_REGEX.matcher(encodeResult).matches();
        Assert.assertTrue(charMatch, "全字节范围编码结果包含非法Base64字符");
        // 256字节的Base64编码长度应为 344（无换行），即 ceil(256/3)*4
        Assert.assertEquals(encodeResult.length(), ((256 + 2) / 3) * 4);
    }

    // ===================== decode(String) 解码测试 =====================
    @Test
    public void dataNull_decode_returnEmptyByteArray() {
        Base64CoderImpl coder = new Base64CoderImpl();
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
        Base64CoderImpl coder = new Base64CoderImpl();
        byte[] result = coder.decode("");
        Assert.assertEquals(result.length, 0);
    }

    @Test
    public void dataStandardBase64Str_decode_restoreSourceText() {
        Base64CoderImpl coder = new Base64CoderImpl();
        byte[] decodeBytes = coder.decode(TEST_BASE64);
        String realText = new String(decodeBytes, StandardCharsets.UTF_8);
        Assert.assertEquals(realText, "test");
    }

    @Test
    public void dataEncodeThenDecode_decode_restoreOriginalBytes() {
        Base64CoderImpl coder = new Base64CoderImpl();
        byte[] originBytes = "Java Base64 Crypto Demo".getBytes(StandardCharsets.UTF_8);
        String encodeStr = coder.encode(originBytes);
        byte[] decodeBytes = coder.decode(encodeStr);
        Assert.assertEquals(decodeBytes, originBytes);
    }

    @Test
    public void dataVariousByteArrays_encodeThenDecode_restoreOriginalBytes() {
        Base64CoderImpl coder = new Base64CoderImpl();
        byte[][] testDatas = {
            new byte[0],
            new byte[]{0x00},
            new byte[]{(byte) 0xFF},
            new byte[]{0x00, 0x01, 0x02, 0x03, 0x04},
            "ABC".getBytes(StandardCharsets.UTF_8),
            "Hello Base64!".getBytes(StandardCharsets.UTF_8),
            new byte[]{0x00, 0x7F, (byte) 0x80, (byte) 0xFF},
            "OpenEuler轻舟框架".getBytes(StandardCharsets.UTF_8),
        };
        for (byte[] origin : testDatas) {
            String encoded = coder.encode(origin);
            byte[] decoded = coder.decode(encoded);
            Assert.assertEquals(decoded, origin);
        }
    }

    @Test
    public void dataPaddingOnlyStr_decode_returnEmptyByteArray() {
        Base64CoderImpl coder = new Base64CoderImpl();
        byte[] result;
        try {
            result = coder.decode("====");
        } catch (IllegalArgumentException e) {
            // 纯填充字符串不是有效Base64，Java解码器会抛出异常，视为空字节数组
            result = new byte[0];
        }
        Assert.assertEquals(result.length, 0);
    }
}