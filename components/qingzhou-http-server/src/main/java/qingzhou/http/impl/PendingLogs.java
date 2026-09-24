package qingzhou.http.impl;

import java.util.ArrayList;
import java.util.List;

import qingzhou.logger.Logger;

// OSGi 动态绑定（ReferencePolicy.DYNAMIC）可能早于 Logger 注入，此期间的消息先暂存，待 @Activate 回放
final class PendingLogs {
    private final List<String> messages = new ArrayList<>();

    synchronized void log(Logger logger, String message) {
        if (logger != null) logger.info(message);
        else messages.add(message);
    }

    synchronized void warn(Logger logger, String message) {
        if (logger != null) logger.warn(message);
        else messages.add(message);
    }

    synchronized void replay(Logger logger) {
        messages.forEach(logger::info);
        messages.clear();
    }
}
