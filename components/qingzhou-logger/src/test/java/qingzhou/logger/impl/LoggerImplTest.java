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
    private static final String CUSTOM_WRITER = LoggerImplTest.class.getName() + "$BufferWriter";

    /**
     * 初始化 debug 级别配置，并等待启动 banner 写入后清空缓冲。
     */
    @BeforeClass
    public static void setUp() throws InterruptedException {
        Map<String, Object> config = new HashMap<>();
        config.put("level", "debug");
        config.put("writer", CUSTOM_WRITER);

        new LoggerImpl().init(config);
        awaitUntil(() -> outputText().contains("'~~"), "startup banner was not written");
        clearOutput();
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

    private static String outputText() {
        synchronized (OUTPUT) {
            return new String(OUTPUT.toByteArray(), StandardCharsets.UTF_8);
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
    public static final class BufferWriter implements Writer {
        public BufferWriter(Map<String, String> configuration) {
        }

        @Override
        public Collection<LogEntryValue> getRequiredLogEntryValues() {
            return EnumSet.of(LogEntryValue.LEVEL, LogEntryValue.MESSAGE, LogEntryValue.EXCEPTION);
        }

        @Override
        public void write(LogEntry logEntry) {
            synchronized (OUTPUT) {
                OUTPUT_STREAM.print(logEntry.getLevel());
                OUTPUT_STREAM.print(" | ");
                OUTPUT_STREAM.println(logEntry.getMessage());
                Throwable exception = logEntry.getException();
                if (exception != null) {
                    exception.printStackTrace(OUTPUT_STREAM);
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
}