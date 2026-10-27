package qingzhou.image.impl;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.image.QrCode;

public class QrCodeImplTest {
    @Test
    public void normal_genImage_getByteArray() throws IOException {
        String text = "012-ABC-中国-繁体-魑魅魍魉-𰻝𰻝面";
        QrCode qrCode = new QrCodeImpl();
        byte[] bytes = qrCode.genImage(text);
        Assert.assertNotNull(bytes);
        Assert.assertTrue(bytes.length > 0);

        String genText = qrCode.genText(text);
        Assert.assertNotNull(genText);
        Assert.assertFalse(genText.trim().isEmpty());
    }
}
