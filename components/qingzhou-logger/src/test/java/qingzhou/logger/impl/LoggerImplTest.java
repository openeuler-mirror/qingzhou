package qingzhou.logger.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.writers.Writer;

/**
 * 覆盖 LoggerImpl 在 debug 级别下的常用日志 API。
 *
 * <p>使用测试类内部的自定义 BufferWriter 接收 tinylog 输出，避免启动 banner 干扰，
 * 也不需要重定向 System.out。各测试方法清空静态缓冲后读取断言。</p>
 */
public class LoggerImplTest {
    private static final long AWAIT_TIMEOUT_MS = 5000L;

    private static final ByteArrayOutputStream OUTPUT = new ByteArrayOutputStream();
    private static final PrintStream OUTPUT_STREAM = new PrintStream(OUTPUT, true);
    private static final ByteArrayOutputStream ACCESS_OUTPUT = new ByteArrayOutputStream();
    private static final PrintStream ACCESS_OUTPUT_STREAM = new PrintStream(ACCESS_OUTPUT, true);
    private static final String CUSTOM_WRITER = LoggerImplTest.class.getName() + "$BufferWriter";
    private static final String ACCESS_WRITER = LoggerImplTest.class.getName() + "$AccessBufferWriter";

    /**
     * 一次性应用「默认 + access」合并配置（模拟生产中多日志器共存），并等待启动 banner 写入后清空缓冲。
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // 测试自持 tinylog 夹具：等价于 TinylogBootstrap 对「默认 + access」的合并产物，
        // 经 tinylog 公共配置入口注入，不触碰生产引导内部状态（tinylog 冻结后不可变更）
        String defaultTag = qingzhou.common.Constants.CONFIG_FACTORY_DEFAULT_INSTANCE;
        Map<String, String> tinylogConfig = new HashMap<>();
        tinylogConfig.put("autoshutdown", "false");
        tinylogConfig.put("level", "debug");
        tinylogConfig.put("writer_" + defaultTag, CUSTOM_WRITER);
        tinylogConfig.put("writer_" + defaultTag + ".tag", defaultTag);
        tinylogConfig.put("writer_access", ACCESS_WRITER);
        tinylogConfig.put("writer_access.tag", "access");
        org.tinylog.configuration.Configuration.replace(tinylogConfig);

        Map<String, Object> config = new HashMap<>();
        config.put("service.pid", "qingzhou-logger");
        new LoggerImpl().init(config);
        awaitUntil(() -> outputText().contains("'~~"), "startup banner was not written");
        clearOutput();
    }

    /**
     * 多日志器路由：默认日志进默认 writer，access 日志进 access writer，互不串流。
     */
    @Test
    public void multiLogger_namedLogger_routesToOwnWriter() throws Exception {
        Map<String, Object> accessProps = new HashMap<>();
        accessProps.put("service.pid", "qingzhou-logger~access");
        LoggerImpl accessLogger = new LoggerImpl();
        accessLogger.init(accessProps);

        clearOutput();
        clearAccessOutput();
        new LoggerImpl().info("default-message");
        accessLogger.info("access-message");

        awaitUntil(() -> outputText().contains("default-message"), "default log was not written");
        awaitUntil(() -> accessOutputText().contains("access-message"), "access log was not written");
        Assert.assertFalse(outputText().contains("access-message"), "access log leaked into default writer");
        Assert.assertFalse(accessOutputText().contains("default-message"), "default log leaked into access writer");
    }

    /**
     * debug 级别下，debug 消息应按 "{level} | {message}" 输出。
     */
    @Test
    public void debugLevel_debugMessage_logsDebugEntry() throws Exception {
        assertLogEntry("debug", "debug-message");
    }

    /**
     * debug 级别下，info 消息应按 "{level} | {message}" 输出。
     */
    @Test
    public void debugLevel_infoMessage_logsInfoEntry() throws Exception {
        assertLogEntry("info", "info-message");
    }

    /**
     * debug 级别下，warn 消息应按 "{level} | {message}" 输出。
     */
    @Test
    public void debugLevel_warnMessage_logsWarnEntry() throws Exception {
        assertLogEntry("warn", "warn-message");
    }

    /**
     * debug 级别下，error 消息应按 "{level} | {message}" 输出。
     */
    @Test
    public void debugLevel_errorMessage_logsErrorEntry() throws Exception {
        assertLogEntry("error", "error-message");
    }

    /**
     * debug 级别下，带异常的 debug 日志应包含异常消息和堆栈。
     */
    @Test
    public void debugLevel_debugWithThrowable_logsStackTrace() throws Exception {
        assertThrowableLog("debug", "debug-throwable");
    }

    /**
     * debug 级别下，带异常的 info 日志应包含异常消息和堆栈。
     */
    @Test
    public void debugLevel_infoWithThrowable_logsStackTrace() throws Exception {
        assertThrowableLog("info", "info-throwable");
    }

    /**
     * debug 级别下，带异常的 warn 日志应包含异常消息和堆栈。
     */
    @Test
    public void debugLevel_warnWithThrowable_logsStackTrace() throws Exception {
        assertThrowableLog("warn", "warn-throwable");
    }

    /**
     * debug 级别下，带异常的 error 日志应包含异常消息和堆栈。
     */
    @Test
    public void debugLevel_errorWithThrowable_logsStackTrace() throws Exception {
        assertThrowableLog("error", "error-throwable");
    }

    /**
     * 格式化日志应严格匹配 "级别 | 内容" 模板。
     */
    @Test
    public void formattedOutput_infoMessage_matchesTemplate() throws Exception {
        clearOutput();
        LoggerImpl logger = new LoggerImpl();
        logger.info("format-message");

        awaitUntil(() -> outputText().contains("INFO | format-message"), "formatted message was not written");
        for (String line : outputText().split("\\r?\\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }
            Assert.assertTrue(line.matches("(DEBUG|INFO|WARN|ERROR) \\| .+"),
                    "unexpected log line: " + line);
        }
    }

    /**
     * debug 级别下，debug/info/warn/error 四个开关均应处于启用状态。
     */
    @Test
    public void debugLevel_switchChecks_allLevelsEnabled() {
        LoggerImpl logger = new LoggerImpl();
        Assert.assertTrue(logger.isDebugEnabled(), "debug should be enabled");
        Assert.assertTrue(logger.isInfoEnabled(), "info should be enabled");
        Assert.assertTrue(logger.isWarnEnabled(), "warn should be enabled");
        Assert.assertTrue(logger.isErrorEnabled(), "error should be enabled");
    }

    /**
     * 同一消息在去重窗口内只输出一条，避免高频失败刷屏。
     */
    @Test
    public void warn_sameMessageWithinWindow_logsOnce() throws Exception {
        clearOutput();
        LoggerImpl logger = new LoggerImpl();
        for (int i = 0; i < 3; i++) {
            logger.warn("dedup-message");
        }

        awaitUntil(() -> outputText().contains("WARN | dedup-message"), "warn log was not written");
        Thread.sleep(1200L); // 等异步写线程把整批任务落盘，确认只写了一条
        int count = 0;
        for (String line : outputText().split("\\r?\\n")) {
            if (line.contains("WARN | dedup-message")) count++;
        }
        Assert.assertEquals(count, 1, "same message should be logged once within the dedup window");
    }

    /**
     * 限流窗口（经外部配置 throttle_window_ms 调小）翻页后应重新放行，并带出上一窗口被抑制的条数。
     */
    @Test
    public void warn_afterThrottleWindow_logsAgainWithSuppressedCount() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("service.pid", "qingzhou-logger");
        config.put("throttle_window_ms", "200");
        LoggerImpl logger = new LoggerImpl();
        logger.init(config);
        awaitUntil(() -> outputText().contains("'~~"), "startup banner was not written");
        clearOutput();

        logger.warn("throttle-rollover-message");
        logger.warn("throttle-rollover-message"); // 窗口内第二条：仅计数
        awaitUntil(() -> countLines("throttle-rollover-message") == 1, "窗口内重复消息应被抑制为一条");

        Thread.sleep(300); // 跨过限流窗口
        logger.warn("throttle-rollover-message");
        awaitUntil(() -> outputText().contains("throttle-rollover-message (suppressed 1 similar)"),
                "窗口翻页后应重新放行并带出抑制条数");
    }

    private static int countLines(String fragment) {
        int count = 0;
        for (String line : outputText().split("\\r?\\n")) {
            if (line.contains(fragment)) count++;
        }
        return count;
    }

    /**
     * 验证指定级别普通日志按配置模板输出。
     */
    private static void assertLogEntry(String level, String message) throws Exception {
        clearOutput();
        LoggerImpl logger = new LoggerImpl();
        invoke(logger, level, message);
        String expected = level.toUpperCase() + " | " + message;
        awaitUntil(() -> outputText().contains(expected), level + " log was not written");
    }

    /**
     * 验证带异常的日志同时包含异常消息、异常类名和测试类堆栈帧。
     */
    private static void assertThrowableLog(String level, String message) throws Exception {
        clearOutput();
        LoggerImpl logger = new LoggerImpl();
        RuntimeException exception = new RuntimeException(message + "-stack-detail");
        invoke(logger, level, message, exception);

        String expected = level.toUpperCase() + " | " + message;
        awaitUntil(() -> outputText().contains(expected), level + " throwable log was not written");

        String text = outputText();
        Assert.assertTrue(text.contains(exception.getMessage()), "throwable message was not written");
        Assert.assertTrue(text.contains(RuntimeException.class.getName()), "throwable class was not written");
        Assert.assertTrue(text.contains("LoggerImplTest"), "stack trace frame was not written");
    }

    /**
     * 按级别调用对应的普通日志方法。
     */
    private static void invoke(LoggerImpl logger, String level, String message) {
        if ("debug".equals(level)) {
            logger.debug(message);
        } else if ("info".equals(level)) {
            logger.info(message);
        } else if ("warn".equals(level)) {
            logger.warn(message);
        } else {
            logger.error(message);
        }
    }

    /**
     * 按级别调用对应的带异常日志方法。
     */
    private static void invoke(LoggerImpl logger, String level, String message, Throwable throwable) {
        if ("debug".equals(level)) {
            logger.debug(message, throwable);
        } else if ("info".equals(level)) {
            logger.info(message, throwable);
        } else if ("warn".equals(level)) {
            logger.warn(message, throwable);
        } else {
            logger.error(message, throwable);
        }
    }

    private static void clearOutput() {
        synchronized (OUTPUT) {
            OUTPUT.reset();
        }
    }

    private static void clearAccessOutput() {
        synchronized (ACCESS_OUTPUT) {
            ACCESS_OUTPUT.reset();
        }
    }

    private static String outputText() {
        synchronized (OUTPUT) {
            return new String(OUTPUT.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String accessOutputText() {
        synchronized (ACCESS_OUTPUT) {
            return new String(ACCESS_OUTPUT.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 轮询等待 tinylog 异步写线程完成输出，超时则测试失败。
     */
    private static void awaitUntil(BooleanSupplier condition, String message) throws InterruptedException {
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        Assert.fail(message);
    }

    /**
     * 自定义 tinylog writer，将日志写入测试类控制的静态内存缓冲。
     */
    public static class BufferWriter implements Writer {
        private final ByteArrayOutputStream out;
        private final PrintStream stream;

        public BufferWriter(Map<String, String> configuration) {
            this(OUTPUT, OUTPUT_STREAM);
        }

        BufferWriter(ByteArrayOutputStream out, PrintStream stream) {
            this.out = out;
            this.stream = stream;
        }

        @Override
        public Collection<LogEntryValue> getRequiredLogEntryValues() {
            return EnumSet.of(LogEntryValue.LEVEL, LogEntryValue.MESSAGE, LogEntryValue.EXCEPTION);
        }

        @Override
        public void write(LogEntry logEntry) {
            synchronized (out) {
                stream.print(logEntry.getLevel());
                stream.print(" | ");
                stream.println(logEntry.getMessage());
                Throwable exception = logEntry.getException();
                if (exception != null) {
                    exception.printStackTrace(stream);
                }
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    /**
     * access 命名日志器的测试 writer，写入独立的内存缓冲以验证路由隔离。
     */
    public static final class AccessBufferWriter extends BufferWriter {
        public AccessBufferWriter(Map<String, String> configuration) {
            super(ACCESS_OUTPUT, ACCESS_OUTPUT_STREAM);
        }
    }
}