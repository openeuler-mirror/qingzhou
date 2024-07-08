package qingzhou.qr.impl;

import io.nayuki.qrcodegen.QrCode;
import io.nayuki.qrcodegen.QrSegment;
import qingzhou.qr.QrGenerator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class QrGeneratorImpl implements QrGenerator {
    @Override
    public byte[] generateQrImage(String qrCode, String format, int scale, int border, int lightColor, int darkColor) throws IOException {
        BufferedImage qrImage = genQrImage(qrCode, scale, border, lightColor, darkColor);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, format, bos);
        return bos.toByteArray();
    }

    /**
     * Returns a raster image depicting the specified QR Code, with the specified module scale, border modules, and module colors.
     * <p>
     * For example, scale=10 and border=4 means to pad the QR Code with 4 light border modules on all four sides, and use 10&#xD7;10 pixels to represent each
     * module.
     *
     * @param qrCode     the QR Code to render (not {@code null})
     * @param scale      the side length (measured in pixels, must be positive) of each module
     * @param border     the number of border modules to add, which must be non-negative
     * @param lightColor the color to use for light modules, in 0xRRGGBB format
     * @param darkColor  the color to use for dark modules, in 0xRRGGBB format
     * @return a new image representing the QR Code, with padding and scaling
     * @throws NullPointerException     if the QR Code is {@code null}
     * @throws IllegalArgumentException if the scale or border is out of range, or if {scale, border, size} cause the image dimensions to exceed
     *                                  Integer.MAX_VALUE
     */
    private BufferedImage genQrImage(String qrCode, int scale, int border, int lightColor, int darkColor) {
        List<QrSegment> segs = QrSegment.makeSegments(qrCode);
        QrCode qr = QrCode.encodeSegments(segs, QrCode.Ecc.HIGH, QrCode.MIN_VERSION, QrCode.MAX_VERSION, -1, true);  // Automatic mask
        Objects.requireNonNull(qr);
        if (scale <= 0 || border < 0) {
            throw new IllegalArgumentException("QrCode Value out of range");
        }
        if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale) {
            throw new IllegalArgumentException("QrCode scale or border too large");
        }

        BufferedImage result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                result.setRGB(x, y, color ? darkColor : lightColor);
            }
        }
        return result;
    }
}
