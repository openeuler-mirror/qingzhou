package qingzhou.app.tomcat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间格式化工具：统一 Tomcat 应用内的时间戳格式，消除各模块散落的
 * {@code new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")} 与 {@code DateTimeFormatter}。
 *
 * <p>使用 {@link DateTimeFormatter}（不可变、线程安全）替代可变且非线程安全的
 * {@link java.text.SimpleDateFormat}。当前仅提供监控常用的 {@code yyyy-MM-dd HH:mm:ss}。
 */
final class DateKit {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateKit() {
    }

    /** 当前时间，格式化为 {@code yyyy-MM-dd HH:mm:ss}。 */
    static String now() {
        return FMT.format(LocalDateTime.now());
    }

    /** 格式化 {@link Date}（如 JMX 返回的进程启动时间）。 */
    static String format(Date date) {
        return FMT.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    /** 格式化 {@link LocalDateTime}（如日志文件最后修改时间）。 */
    static String format(LocalDateTime ldt) {
        return FMT.format(ldt);
    }
}
