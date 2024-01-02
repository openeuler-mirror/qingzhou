package qingzhou.framework.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeUtil {
    public static final String CURRENT_TIME_THREAD_NAME = "QingZhou-Time-Cache";
    public static final String FILE_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH.mm.ss";

    // 移植自：tomee.TransactionTimer
    private static volatile long currentTime;

    static {
        CurrentTime tm = new CurrentTime();
        tm.setDaemon(true);
        tm.start();
    }

    public static long getCurrentTime() {
        return System.currentTimeMillis();
    }

    private static class CurrentTime extends Thread {
        protected CurrentTime() {
            currentTime = System.currentTimeMillis();
            this.setContextClassLoader(null);
            this.setName(CURRENT_TIME_THREAD_NAME);
        }

        public void run() {
            while (true) {
                currentTime = System.currentTimeMillis();
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    // Ignore exception
                }
            }
        }
    }

    /**
     * 计算两个时间差（年，月，星期，日，时，分，秒）
     *
     * @param startTimeMillis
     * @param endTimeMillis
     * @return
     */
    public static String calculateTimeDifference(long startTimeMillis, long endTimeMillis) {
        Date startDate = new Date();
        startDate.setTime(startTimeMillis);
        Date ednDate = new Date();
        ednDate.setTime(endTimeMillis);
        return calculateTimeDifference(startDate, ednDate);
    }

    /**
     * 计算两个时间差（年，月，星期，日，时，分，秒）
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static String calculateTimeDifference(Date startDate, Date endDate) {
        if (null == startDate || null == endDate) {
            return "";
        }
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime fromDateTime = LocalDateTime.ofInstant(startDate.toInstant(), zoneId);
        LocalDateTime toDateTime = LocalDateTime.ofInstant(endDate.toInstant(), zoneId);

        LocalDateTime tempDateTime = LocalDateTime.from(fromDateTime);

        long years = tempDateTime.until(toDateTime, ChronoUnit.YEARS);
        tempDateTime = tempDateTime.plusYears(years);

        long months = tempDateTime.until(toDateTime, ChronoUnit.MONTHS);
        tempDateTime = tempDateTime.plusMonths(months);

        long days = tempDateTime.until(toDateTime, ChronoUnit.DAYS);
        tempDateTime = tempDateTime.plusDays(days);

        long hours = tempDateTime.until(toDateTime, ChronoUnit.HOURS);
        tempDateTime = tempDateTime.plusHours(hours);

        long minutes = tempDateTime.until(toDateTime, ChronoUnit.MINUTES);
        tempDateTime = tempDateTime.plusMinutes(minutes);

        long seconds = tempDateTime.until(toDateTime, ChronoUnit.SECONDS);

        return (0 == years ? "" : years + " years ")
                + (0 == months ? "" : months + " months ")
                + (0 == days ? "" : days + " days ")
                + (0 == hours ? "" : hours + " hours ")
                + (0 == minutes ? "" : minutes + " minutes ")
                + (0 == seconds ? "" : seconds + " seconds");
    }

    public static String getFormatDateTime(String pattern) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    private TimeUtil() {
    }
}
