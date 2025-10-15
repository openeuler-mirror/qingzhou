package qingzhou.qr;

import qingzhou.engine.Service;

import java.io.IOException;

@Service(name = "QR Code Generator", description = "Convert the data into a simple QR code image format.")
public interface QrGenerator {
    byte[] generateQrImage(String qrCode, String format, int scale, int border, int lightColor, int darkColor) throws IOException;
}
