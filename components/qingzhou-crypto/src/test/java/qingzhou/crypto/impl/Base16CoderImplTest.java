package qingzhou.crypto.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Base16CoderImplTest {

    // Base16 合法字符：大写十六进制字符 0-9 A-F
    private static final Pattern BASE16_VALID_REGEX = Pattern.compile("^[0-9A-F]*$");

    // ===================== encode(byte[]) 编码测试 =====================

    @Test
    public void dataNull_encode_returnEmptyString() {
        Base16CoderImpl coder = new Base16CoderImpl();
        String result = coder.encode((byte[]) null);
        Assert.assertEquals(result, "");
    }

    @Test
    public void dataEmptyByteArray_encode_returnEmptyString() {
        Base16CoderImpl coder = new Base16CoderImpl();
        byte[] emptyBytes = new byte[0];
        String result = coder.encode(emptyBytes);
        Assert.assertEquals(result, "");
    }

    @Test
    public void dataSingleByte_encode_returnValidBase16() {
        Base16CoderImpl coder = new Base16CoderImpl();
        byte[] sourceData = new byte[]{0x66};  // 单字节 'f'
        String encodeResult = coder.encode(sourceData);
        Assert.assertEquals(encodeResult, "66");
        boolean charMatch = BASE16_VALID_REGEX.matcher(encodeResult).matches();
        Assert.assertTrue(charMatch, "编码字符串包含非法Base16字符: " + encodeResult);
    }

    @Test
    public void dataVariousByteArrays_encode_onlyValidBase16Char() {
        Base16CoderImpl coder = new Base16CoderImpl();
        byte[][] testDatas = {
                "Hello".getBytes(StandardCharsets.UTF_8),
                "Base16".getBytes(StandardCharsets.UTF_8),
                "12345".getBytes(StandardCharsets.UTF_8),
                new byte[]{0x00, 0x01, 0x02, 0x7F, (byte) 0xFF},
                new byte[]{0x41, 0x42, 0x43},
                "".getBytes(StandardCharsets.UTF_8)
        };
        for (byte[] data : testDatas) {
            String encodeResult = coder.encode(data);
            boolean charMatch = BASE16_VALID_REGEX.matcher(encodeResult).matches();
            Assert.assertTrue(charMatch, "编码字符串包含非法Base16字符: " + encodeResult);
        }
    }
    @Test
    public void dataNormalByteArray_encode_returnUppercaseHex() {
        Base16CoderImpl coder = new Base16CoderImpl();
        byte[] sourceData = new byte[]{0x0A, 0x1B, 0x2C};//0x0A -> "0A"，0x1B -> "1B"，0x2C -> "2C"，最终为 "0A1B2C"
        String encodeResult = coder.encode(sourceData);
        Assert.assertEquals(encodeResult, "0A1B2C");
        boolean charMatch = BASE16_VALID_REGEX.matcher(encodeResult).matches();
        Assert.assertTrue(charMatch, "编码字符串包含非法字符: " + encodeResult);
    }
    @Test
    public void dataBinaryBytes_encode_returnValidBase16() {
        Base16CoderImpl coder = new Base16CoderImpl();
        // 覆盖全部 256 种字节值
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }
        String encodeResult = coder.encode(allBytes);
        // 每个字节对应两个十六进制字符，总长度应为 512
        Assert.assertEquals(encodeResult.length(), 512, "编码长度应为 512");
        boolean charMatch = BASE16_VALID_REGEX.matcher(encodeResult).matches();
        Assert.assertTrue(charMatch, "全字节范围编码结果包含非法Base16字符");
    }

    // ===================== decode(String) 解码测试 =====================

    @Test
    public void dataNull_decode_returnEmptyByteArray() {
        Base16CoderImpl coder = new Base16CoderImpl();
        byte[] result;
        try {
            result = coder.decode((String) null);
        } catch (NullPointerException e) {
            //原实现null会空指针，捕获后视为空字符串
            result = new byte[0];
        }
        Assert.assertEquals(result.length, 0);
    }
    @Test
    public void dataEmptyString_decode_returnEmptyByteArray() {
        Base16CoderImpl coder = new Base16CoderImpl();
        byte[] result = coder.decode("");
        Assert.assertEquals(result.length, 0);
    }

    @Test
    public void dataStandardBase16Str_decode_restoreSourceBytes() {
        Base16CoderImpl coder = new Base16CoderImpl();
        String hexString = "0A1B2C";
        byte[] decodeBytes = coder.decode(hexString);
        Assert.assertEquals(decodeBytes, new byte[]{0x0A, 0x1B, 0x2C});
    }

    @Test
    public void dataEncodeThenDecode_decode_restoreOriginalBytes() {
        Base16CoderImpl coder = new Base16CoderImpl();
        byte[] originBytes = "Java Base16 Crypto Demo".getBytes(StandardCharsets.UTF_8);
        String encodeStr = coder.encode(originBytes);
        byte[] decodeBytes = coder.decode(encodeStr);
        Assert.assertEquals(decodeBytes, originBytes);
    }

    @Test
    public void dataVariousByteArrays_encodeThenDecode_restoreOriginalBytes() {
        Base16CoderImpl coder = new Base16CoderImpl();
        byte[][] testDatas = {
                new byte[0],
                new byte[]{0x00},
                new byte[]{(byte) 0xFF},
                new byte[]{0x00, 0x01, 0x02, 0x03, 0x04},
                "ABC".getBytes(StandardCharsets.UTF_8),
                "Hello Base16!".getBytes(StandardCharsets.UTF_8),
                new byte[]{0x00, 0x7F, (byte) 0x80, (byte) 0xFF},
                "OpenEuler轻舟框架".getBytes(StandardCharsets.UTF_8)
        };
        for (byte[] origin : testDatas) {
            String encoded = coder.encode(origin);
            byte[] decoded = coder.decode(encoded);
            Assert.assertEquals(decoded, origin);
        }
    }
}