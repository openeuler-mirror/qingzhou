package qingzhou.logger.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class Slf4jBridge implements SLF4JServiceProvider, ILoggerFactory {

    private static volatile qingzhou.logger.Logger qingzhouLogger;
    private final ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<>();
    private final IMarkerFactory markerFactory = new BasicMarkerFactory();
    private final MDCAdapter mdcAdapter = new BasicMDCAdapter();

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
        // Logger is set externally via Slf4jBridgeActivator
    }

    @Override
    public Logger getLogger(String name) {
        return loggerMap.computeIfAbsent(name, Slf4jLogger::new);
    }

    static qingzhou.logger.Logger getQingzhouLogger() {
        return qingzhouLogger;
    }

    static void setQingzhouLogger(qingzhou.logger.Logger logger) {
        qingzhouLogger = logger;
    }
}
