package qingzhou.qr;

import qingzhou.engine.ServiceInfo;

import java.io.IOException;

public interface QrGenerator extends ServiceInfo {
    @Override
    default String getDescription() {
        return "Provide practical tools related to QR code generation.";
    }

    byte[] generateQrImage(String qrCode, String format, int scale, int border, int lightColor, int darkColor) throws IOException;
}
