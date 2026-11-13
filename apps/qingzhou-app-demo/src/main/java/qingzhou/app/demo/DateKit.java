package qingzhou.app.demo;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 统一时间格式化工具。
 *
 * <p>列表、详情、监控页的时间字段共用同一格式；本类使用 {@code java.time} 的不可变
 * {@link DateTimeFormatter}（线程安全），避免 {@code SimpleDateFormat} 作为共享可变状态的并发隐患。
 */
final class DateKit {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter NUM = DateTimeFormatter.ofPattern("yyyyMMdd");

    private DateKit() {
    }

    /** 标准展示时间：{@code yyyy-MM-dd HH:mm:ss}。 */
    static String now() {
        return LocalDateTime.now().format(FMT);
    }

    /** 纯数字日期：{@code yyyyMMdd}，用于拼装订单号等业务主键。 */
    static String dateNum() {
        return LocalDate.now().format(NUM);
    }
}
