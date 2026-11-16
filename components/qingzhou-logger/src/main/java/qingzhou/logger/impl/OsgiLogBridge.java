package qingzhou.logger.impl;

import java.util.Enumeration;

import org.osgi.framework.Bundle;
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
        // 必须先注册监听再读缓冲区：getLog() 返回的是调用时刻的快照，反序则两次调用之间产生的日志既不在快照内也不会回调，永久丢失
        logReaderService.addLogListener(this);
        long registeredAt = System.currentTimeMillis();

        // 输出缓冲区中的历史日志（features 阶段先于 logger 启动，此期间的 OSGI 日志暂存于缓冲区）
        Enumeration<LogEntry> history = logReaderService.getLog();
        while (history.hasMoreElements()) {
            LogEntry entry = history.nextElement();
            if (entry.getTime() < registeredAt) { // 注册后产生的条目已由回调输出，跳过以免重复
                logged(entry);
            }
        }
    }

    @Deactivate
    public void deactivate() {
        logReaderService.removeLogListener(this);
    }

    @Override
    public void logged(LogEntry entry) {
        String message = formatMessage(entry);
        Throwable exception = entry.getException();

        switch (entry.getLevel()) { // 仅输出 ERROR 和 WARN 级别的日志，不然 INFO 和 DEBUG 日志太多无用信息
            case org.osgi.service.log.LogService.LOG_ERROR:
                if (exception != null) logger.error(message, exception);
                else logger.error(message);
                break;
            case org.osgi.service.log.LogService.LOG_WARNING:
                if (exception != null) logger.warn(message, exception);
                else logger.warn(message);
                break;
        }
    }

    private static String formatMessage(LogEntry entry) {
        // 契约适配：bundle 卸载后 getBundle() 返回 null，且 getSymbolicName() 本身允许 null，故取 bundleId 兜底
        Bundle bundle = entry.getBundle();
        if (bundle == null) return "[OSGI][unknown] " + entry.getMessage();
        String bundleName = bundle.getSymbolicName();
        return "[OSGI][" + (bundleName != null ? bundleName : "#" + bundle.getBundleId()) + "] " + entry.getMessage();
    }
}
