package qingzhou.uml;

import qingzhou.engine.ServiceInfo;

import java.io.IOException;

public interface Uml extends ServiceInfo {
    @Override
    default String getDescription() {
        return "Provide practical tools related to UML-related image generation.";
    }

    /**
     * 生成 SVG 图像
     */
    byte[] toSvg(String source) throws IOException;
}
