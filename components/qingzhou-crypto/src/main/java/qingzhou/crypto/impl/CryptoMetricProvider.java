package qingzhou.crypto.impl;

import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;

/**
 * 加解密健康度：解密失败会把密文当明文使用，是配置密钥错配最危险的静默降级，须可聚合告警。
 */
@Component(property = MetricProvider.PROVIDER_ID + "=crypto")
public class CryptoMetricProvider implements MetricProvider {

    @Override
    public Set<Metric> collectMetrics() {
        Set<Metric> metrics = new HashSet<>();
        metrics.add(Metric.of("decrypt_fallbacks_total", CipherImpl.decryptFallbacks(), Metric.Type.COUNTER,
                "Ciphertexts that failed to decrypt and were used as raw values"));
        return metrics;
    }
}
