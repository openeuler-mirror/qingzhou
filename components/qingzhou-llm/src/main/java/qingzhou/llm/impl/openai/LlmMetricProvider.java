package qingzhou.llm.impl.openai;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;

/**
 * 模型侧成本与健康度：token 消耗是唯一可折算费用的指标，失败与重试是外部服务健康度的直接信号。
 */
@Component(property = MetricProvider.PROVIDER_ID + "=llm")
public class LlmMetricProvider implements MetricProvider {
    // 同名族须共用说明，否则端点在渲染时判定为冲突
    private static final String TOKENS_DESC = "Tokens consumed by streaming LLM calls, split by prompt and completion";
    private static final String FAILURES_DESC = "LLM requests that ended in failure, by status code or error kind";

    @Override
    public Set<Metric> collectMetrics() {
        Set<Metric> metrics = new HashSet<>();
        metrics.add(Metric.of("requests_total", LlmMetrics.requests(), Metric.Type.COUNTER,
                "LLM HTTP requests sent, retries included"));
        metrics.add(Metric.of("retries_total", LlmMetrics.retries(), Metric.Type.COUNTER,
                "LLM requests re-sent after a retryable failure"));
        metrics.add(Metric.of("stream_interruptions_total", LlmMetrics.streamInterruptions(), Metric.Type.COUNTER,
                "Streaming responses interrupted before completion"));
        metrics.add(Metric.of("tokens_total", LlmMetrics.promptTokens(), Metric.Type.COUNTER,
                Collections.singletonMap("type", "prompt"), TOKENS_DESC));
        metrics.add(Metric.of("tokens_total", LlmMetrics.completionTokens(), Metric.Type.COUNTER,
                Collections.singletonMap("type", "completion"), TOKENS_DESC));
        for (Map.Entry<String, Long> failure : LlmMetrics.failures().entrySet()) {
            metrics.add(Metric.of("failures_total", failure.getValue(), Metric.Type.COUNTER,
                    Collections.singletonMap("reason", failure.getKey()), FAILURES_DESC));
        }
        return metrics;
    }
}
