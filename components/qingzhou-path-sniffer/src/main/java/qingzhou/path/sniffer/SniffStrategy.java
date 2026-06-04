package qingzhou.path.sniffer;

import java.util.List;

/**
 * 本机应用程序安装路径探测策略统一接口
 */
public interface SniffStrategy {

    /**
     * 优先级，数字越小越先执行
     *
     * @return
     */
    int getPriority();

    /**
     * 执行探测
     *
     * @param profile
     * @return 结果列表，空表示未探测到
     */
    List<PathResult> sniff(ApplicationProfile profile);
}
