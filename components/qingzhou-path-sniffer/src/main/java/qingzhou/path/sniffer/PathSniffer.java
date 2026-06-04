package qingzhou.path.sniffer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import qingzhou.path.sniffer.strategy.CandidateScanStrategy;
import qingzhou.path.sniffer.strategy.CommandStrategy;
import qingzhou.path.sniffer.strategy.EnvVarStrategy;
import qingzhou.path.sniffer.strategy.ProcessStrategy;
import qingzhou.path.sniffer.strategy.ServiceStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 采用多策略探测应用程序安装路径
 */
@Component(service = PathSniffer.class, configurationPid = "qingzhou-app-detector", configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class PathSniffer {

    private final List<SniffStrategy> strategies;

    public PathSniffer() {
        this.strategies = Arrays.asList(
                new EnvVarStrategy(),
                new ProcessStrategy(),
                new ServiceStrategy(),
                new CommandStrategy(),
                new CandidateScanStrategy()
        );
        // 按优先级排序
        strategies.sort(Comparator.comparingInt(SniffStrategy::getPriority));
    }
    
    /**
     * 统一探测入口
     *
     * @param profile
     * @return
     */
    public List<PathResult> sniff(ApplicationProfile profile) {
        List<PathResult> result = new ArrayList<>();
        for (SniffStrategy strategy : strategies) {
            try {
                result.addAll(strategy.sniff(profile));
                if (!result.isEmpty() && profile.isStopOnHit()) {
                    return result;
                }
            } catch (Exception ignored) {
            }
        }

        return result;
    }
}
