package qingzhou.image;

import java.io.IOException;

public interface QrCode {
    // scale: 小格子的大小，根据 text 内容多少和需要显示的二维码大小来选值，一般为 10
    byte[] genImage(String text, int scale) throws IOException;
}
