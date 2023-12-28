package qingzhou.console.audit;

import qingzhou.console.controller.RestContext;
import qingzhou.console.controller.SystemController;
import qingzhou.console.sdk.ConsoleSDK;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.framework.pattern.Filter;
import qingzhou.console.util.ThreadUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class AuditFilter implements Filter<RestContext> {
    private static volatile AuditInterface auditInterface;
    private static volatile ExecutorService executorService;

    public static AuditInterface.SearchResult read(int pageSize, int pageNum, Map<String, String> filterParams, Cache cache) throws Exception {
        return auditInterface.read(pageSize, pageNum, filterParams, cache);
    }

    // 外部使用
    public static void auditLog(LogLine logLine) {
        if (logLine.get(LogField.user) == null) {
            return;
        }

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String requestPath = logLine.get(LogField.uri);
        int i = requestPath.lastIndexOf("/");
        if (i > -1) {
            String checkId = requestPath.substring(i + 1);
            if (ConsoleSDK.isEncodedId(checkId)) {
                requestPath = requestPath.substring(0, i + 1) + ConsoleSDK.decodeId(checkId);
            }
        }
        String finalRequestPath = requestPath;
        logLine.set(LogField.uri, finalRequestPath);
        logLine.set(LogField.time, time);

        if (auditInterface != null) {
            executorService().execute(() -> auditInterface.write(logLine));
        }
    }

    private static ExecutorService executorService() {
        if (executorService == null) {
            synchronized (AuditFilter.class) {
                if (executorService == null) {
                    executorService = ThreadUtil.newExecutorService(AuditFilter.class.getSimpleName(), 1);
                    SystemController.addShutdownHook(() -> executorService.shutdownNow());
                }
            }
        }
        return executorService;
    }

    // 日志系统参数变化了，接收变更事件
    public static void reload() {
        auditInterface.reload();
    }

    private void audit(Request request, Response response) {
        // 进步异步后下面值将会被重置，故先得之再异步。
        String loginUser = request.getLoginUser();
        if (loginUser == null) { // fix #ITAIT-2851，非登录请求，如 加密工具的开放加密接口
            return;
        }
        String actionName = request.getActionName();
        String modelName = request.getModelName();
        String result = String.valueOf(response.isSuccess());
        String clientIp = request.getClientIp();
        String requestPath = request.getUri();
        LogLine logLine = new LogLine();
        logLine.set(LogField.user, loginUser)
                .set(LogField.model, modelName)
                .set(LogField.action, actionName)
                .set(LogField.result, result)
                .set(LogField.uri, requestPath)
                .set(LogField.clientIp, clientIp);
        auditLog(logLine);
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        return true;
    }

    @Override
    public void afterFilter(RestContext context) {
        audit(context.request, context.response);
    }

    public enum LogField {
        user, model, action, result, uri, clientIp, time
    }

    public interface Cache {
        void setCache(String key, Object cache);

        Object getCache(String key);
    }

    public static class LogLine {
        private final String[] dataS = new String[LogField.values().length];

        public LogLine set(LogField logField, String data) {
            dataS[logField.ordinal()] = data;
            return this;
        }

        public String get(LogField logField) {
            return dataS[logField.ordinal()];
        }
    }
}
