package qingzhou.qr;

import java.io.IOException;

public interface QrGenerator {
    byte[] generateQrImage(String qrCode, String format, int scale, int border, int lightColor, int darkColor) throws IOException;
}
