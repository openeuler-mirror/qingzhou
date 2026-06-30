package qingzhou.image.impl;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.image.Captcha;

public class CaptchaImplTest {
    @Test
    public void normal_genImage_getByteArray() throws IOException {
        Captcha captcha = new CaptchaImpl();
        String genCode = captcha.genCode();
        byte[] bytes = captcha.genImage(genCode);
        Assert.assertNotNull(bytes);
        Assert.assertTrue(bytes.length > 0);
    }
}
