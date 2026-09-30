package qingzhou.http.server.util;

import java.util.ArrayList;
import java.util.List;

import qingzhou.logger.Logger;

// OSGi 动态绑定（ReferencePolicy.DYNAMIC）可能早于 Logger 注入，此期间的消息先暂存，待 @Activate 回放
public final class PendingLogs {
    private final List<String> messages = new ArrayList<>();

    public synchronized void log(Logger logger, String message) {
        if (logger != null) logger.info(message);
        else messages.add(message);
    }

    public synchronized void warn(Logger logger, String message) {
        if (logger != null) logger.warn(message);
        else messages.add(message);
    }

    public synchronized void replay(Logger logger) {
        if (logger == null) return; // 保留暂存消息，待 logger 可用后再回放
        messages.forEach(logger::info);
        messages.clear();
    }
}
