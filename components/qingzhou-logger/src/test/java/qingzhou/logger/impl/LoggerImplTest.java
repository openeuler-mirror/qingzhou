package qingzhou.logger.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 覆盖 LoggerImpl 在 debug 级别下的常用日志 API。
 *
 * <p>本类只使用 debug 级别初始化，可覆盖 debug/info/warn/error 输出、异常堆栈、
 * 格式化输出以及开关查询 API。</p>
 */
public class LoggerImplTest {
    private static final long AWAIT_TIMEOUT_MS = 5000L;

    /**
     * 初始化 debug 级别配置，并吞掉启动 banner，避免干扰后续日志断言。
     */
    @BeforeClass
    public static void setUp() throws InterruptedException {
        Map<String, Object> config = new HashMap<>();
        config.put("level", "debug");
        config.put("writer", "console");
        config.put("writer.format", "{level} | {message}");
        config.put("writer.stream", "out");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream startupOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(startupOutput, true));
        try {
            new LoggerImpl().init(config);
            awaitUntil(() -> textOf(startupOutput).contains("'~~"), "startup banner was not written");
        } finally {
            System.setOut(originalOut);
        }
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
        try (OutputCapture capture = new OutputCapture()) {
            LoggerImpl logger = new LoggerImpl();
            logger.info("format-message");

            awaitUntil(() -> capture.outText().contains("INFO | format-message"), "formatted message was not written");
            for (String line : capture.outText().split("\\r?\\n")) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                Assert.assertTrue(line.matches("(DEBUG|INFO|WARN|ERROR) \\| .+"),
                        "unexpected log line: " + line);
            }
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
        try (OutputCapture capture = new OutputCapture()) {
            LoggerImpl logger = new LoggerImpl();
            invoke(logger, level, message);
            String expected = level.toUpperCase() + " | " + message;
            awaitUntil(() -> capture.outText().contains(expected), level + " log was not written");
        }
    }

    /**
     * 验证带异常的日志同时包含异常消息、异常类名和测试类堆栈帧。
     */
    private static void assertThrowableLog(String level, String message) throws Exception {
        try (OutputCapture capture = new OutputCapture()) {
            LoggerImpl logger = new LoggerImpl();
            RuntimeException exception = new RuntimeException(message + "-stack-detail");
            invoke(logger, level, message, exception);

            String expected = level.toUpperCase() + " | " + message;
            awaitUntil(() -> capture.outText().contains(expected), level + " throwable log was not written");

            String text = capture.outText();
            Assert.assertTrue(text.contains(exception.getMessage()), "throwable message was not written");
            Assert.assertTrue(text.contains(RuntimeException.class.getName()), "throwable class was not written");
            Assert.assertTrue(text.contains("LoggerImplTest"), "stack trace frame was not written");
        }
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

    private static String textOf(ByteArrayOutputStream buffer) {
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
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
     * 临时接管 System.out/System.err，将日志写入内存并在关闭时恢复原流。
     */
    private static final class OutputCapture implements AutoCloseable {
        private final PrintStream originalOut = System.out;
        private final PrintStream originalErr = System.err;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        OutputCapture() {
            System.setOut(new PrintStream(out, true));
            System.setErr(new PrintStream(err, true));
        }

        String outText() {
            return textOf(out);
        }

        @Override
        public void close() {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
}