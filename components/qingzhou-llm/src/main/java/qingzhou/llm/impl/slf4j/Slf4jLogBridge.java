package qingzhou.llm.impl.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class Slf4jLogBridge implements SLF4JServiceProvider, ILoggerFactory {
    public static qingzhou.logger.Logger qingzhouLogger; // 框架注入

    private final IMarkerFactory markerFactory = new BasicMarkerFactory();
    private final MDCAdapter mdcAdapter = new NOPMDCAdapter();

    @Override
    public ILoggerFactory getLoggerFactory() {
        return this;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.17";
    }

    @Override
    public void initialize() {
    }

    @Override
    public Logger getLogger(String name) {
        return new Slf4jLogger(name, qingzhouLogger);
    }
}
