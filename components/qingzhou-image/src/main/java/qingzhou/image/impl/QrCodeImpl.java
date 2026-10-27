package qingzhou.image.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import io.nayuki.qrcodegen.QrSegment;
import org.osgi.service.component.annotations.Component;
import qingzhou.image.QrCode;

@Component
public class QrCodeImpl implements QrCode {
    // 每个模块的像素边长：10 在清晰度与图片体积间较均衡，为常见取值
    private static final int IMAGE_SCALE = 10;
    // 静区取规范值 4 模块：贴近终端边缘或屏幕反光时仍可被识别
    private static final int TEXT_BORDER = 4;
    // 显式黑白取色：深色终端主题下若沿用默认前景色，暗模块会被画成亮色导致扫描失败
    private static final String BLACK_ON_WHITE = "\033[30;47m";
    private static final String RESET = "\033[0m";

    @Override
    public byte[] genImage(String qrCode) throws IOException {
        io.nayuki.qrcodegen.QrCode qr = encode(qrCode);
        int modules = qr.size + 2; // 图片静区沿用 1 模块，不改变既有 PNG 输出
        BufferedImage image = new BufferedImage(modules * IMAGE_SCALE, modules * IMAGE_SCALE, BufferedImage.TYPE_INT_RGB);
        // 按模块整块填充而非逐像素绘制：避免 O(像素数) 的逐点 setRGB
        java.awt.Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(java.awt.Color.BLACK);
            for (int y = 0; y < qr.size; y++) {
                for (int x = 0; x < qr.size; x++) {
                    if (qr.getModule(x, y)) {
                        graphics.fillRect((x + 1) * IMAGE_SCALE, (y + 1) * IMAGE_SCALE, IMAGE_SCALE, IMAGE_SCALE);
                    }
                }
            }
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bos);
        return bos.toByteArray();
    }

    @Override
    public String genText(String qrCode) {
        io.nayuki.qrcodegen.QrCode qr = encode(qrCode);
        int size = qr.size + TEXT_BORDER * 2;
        // 终端字符高宽比约 2:1：一个字符承载上下两个模块，图形比例还原为正方
        StringBuilder sb = new StringBuilder(BLACK_ON_WHITE);
        for (int y = 0; y < size; y += 2) {
            for (int x = 0; x < size; x++) {
                boolean top = isDark(qr, x, y);
                boolean bottom = isDark(qr, x, y + 1);
                sb.append(top ? (bottom ? '█' : '▀') : (bottom ? '▄' : ' '));
            }
            sb.append('\n');
        }
        return sb.append(RESET).toString();
    }

    private static boolean isDark(io.nayuki.qrcodegen.QrCode qr, int x, int y) {
        int moduleX = x - TEXT_BORDER;
        int moduleY = y - TEXT_BORDER;
        return moduleX >= 0 && moduleY >= 0 && moduleX < qr.size && moduleY < qr.size && qr.getModule(moduleX, moduleY);
    }

    private static io.nayuki.qrcodegen.QrCode encode(String qrCode) {
        if (qrCode == null) throw new NullPointerException("qrCode required");
        List<QrSegment> segs = QrSegment.makeSegments(qrCode);
        // Automatic mask
        return io.nayuki.qrcodegen.QrCode.encodeSegments(segs, io.nayuki.qrcodegen.QrCode.Ecc.HIGH,
                io.nayuki.qrcodegen.QrCode.MIN_VERSION, io.nayuki.qrcodegen.QrCode.MAX_VERSION, -1, true);
    }
}
