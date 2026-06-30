package qingzhou.image;

import java.io.IOException;

public interface Captcha {
    String genCode();

    byte[] genImage(String code) throws IOException;
}
