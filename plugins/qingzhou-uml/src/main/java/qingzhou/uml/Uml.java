package qingzhou.uml;

import java.io.IOException;

public interface Uml {
    /**
     * 生成 SVG 图像
     */
    byte[] toSvg(String source) throws IOException;
}
