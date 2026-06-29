package qingzhou.image.impl;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.image.QrCode;

public class QrCodeImplTest {
    @Test
    public void normal_genImage_getByteArray() throws IOException {
        String text = "otpauth://totp/USER_id?secret=secretBase32_secretBase32_secretBase32_xxxxxx";
        QrCode qrCode = new QrCodeImpl();
        byte[] bytes = qrCode.genImage(text, 10);
        Assert.assertNotNull(bytes);
        Assert.assertTrue(bytes.length > 0);
    }
}
