package qingzhou.uml;

import qingzhou.engine.Service;

import java.io.IOException;

@Service(name = "UML Generator", description = "Convert data that meets the specifications into UML image format.")
public interface Uml {
    /**
     * 生成 SVG 图像
     */
    byte[] toSvg(String source) throws IOException;
}
