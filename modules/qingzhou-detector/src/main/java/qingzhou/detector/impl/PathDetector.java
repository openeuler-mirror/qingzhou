package qingzhou.detector.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import qingzhou.detector.ApplicationProfile;
import qingzhou.detector.DetectionStrategy;
import qingzhou.detector.PathResult;
import qingzhou.detector.impl.strategy.*;

@Component
public class PathDetector {

    private final List<DetectionStrategy> strategies;

    public PathDetector() {
        this.strategies = Arrays.asList(
                new EnvVarStrategy(),
                new ProcessStrategy(),
                new ServiceStrategy(),
                new CommandStrategy(),
                new CandidateScanStrategy()
        );
        // 按优先级排序
        strategies.sort(Comparator.comparingInt(DetectionStrategy::getPriority));
    }

    /**
     * 统一探测入口
     */
    public List<PathResult> detect(ApplicationProfile profile) {
        List<PathResult> result = new ArrayList<>();
        for (DetectionStrategy strategy : strategies) {
            try {
                result.addAll(strategy.detect(profile));
                if (!result.isEmpty() && profile.isStopOnHit()) {
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        return new ArrayList<>(
                result.stream().collect(
                        Collectors.toMap(
                                ps -> ps.getPath().toAbsolutePath().toString(),
                                pr -> pr,
                                (pr1, pr2) -> pr1.getStrategy().getPriority() <= pr2.getStrategy().getPriority() ? pr1 : pr2
                        )).values()
        );
    }
}
