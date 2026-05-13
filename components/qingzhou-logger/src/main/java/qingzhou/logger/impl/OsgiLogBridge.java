package qingzhou.logger.impl;

import java.util.Enumeration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import qingzhou.logger.Logger;

@Component(immediate = true)
public class OsgiLogBridge implements LogListener {
    @Reference
    private Logger logger;

    @Reference
    private LogReaderService logReaderService;

    @Activate
    public void activate() {
        // 输出 LogReaderService 缓冲区中的历史日志（features 阶段在 logger 之前启动，此时 OSGI 日志暂存在缓冲区中）
        Enumeration<LogEntry> history = logReaderService.getLog();
        if (history != null) {
            while (history.hasMoreElements()) {
                logged(history.nextElement());
            }
        }
        logReaderService.addLogListener(this);
    }

    @Deactivate
    public void deactivate() {
        logReaderService.removeLogListener(this);
    }

    @Override
    public void logged(LogEntry entry) {
        String message = formatMessage(entry);
        Throwable exception = entry.getException();

        switch (entry.getLevel()) {
//            case org.osgi.service.log.LogService.LOG_DEBUG:
//                logger.debug(message, exception);
//                break;
//            case org.osgi.service.log.LogService.LOG_INFO:
//                logger.info(message, exception);
//                break;
            case org.osgi.service.log.LogService.LOG_WARNING:
                logger.warn(message, exception);
                break;
            case org.osgi.service.log.LogService.LOG_ERROR:
                logger.error(message, exception);
                break;
        }
    }

    private static String formatMessage(LogEntry entry) {
        String bundleName = entry.getBundle() != null ? entry.getBundle().getSymbolicName() : "unknown";
        return "[OSGI][" + bundleName + "] " + entry.getMessage();
    }
}
