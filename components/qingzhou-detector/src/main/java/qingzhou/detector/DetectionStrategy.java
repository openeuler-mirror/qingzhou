package qingzhou.detector;

import java.util.List;

/**
 * 本机应用程序安装路径探测策略统一接口
 */
public interface DetectionStrategy {

    /**
     * 优先级，数字越小越先执行
     */
    int getPriority();

    /**
     * 执行探测
     *
     * @return 结果列表，空表示未探测到
     */
    List<PathResult> detect(ApplicationProfile profile);
}
