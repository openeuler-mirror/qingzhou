package qingzhou.engine.detector;

import java.util.List;

interface PathDetectionStrategy {
    List<String> detect() throws Exception;
}
