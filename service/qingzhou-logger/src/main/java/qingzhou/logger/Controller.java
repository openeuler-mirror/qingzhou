package qingzhou.logger;

import qingzhou.bootstrap.main.ServiceRegister;
import qingzhou.framework.logger.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class Controller extends ServiceRegister<Logger> {
    private Logger logger;
    private long startTime;

    @Override
    public Class<Logger> serviceType() {
        return Logger.class;
    }

    @Override
    protected Logger serviceObject() {
        return logger;
    }

    @Override
    protected void startService() throws Exception {
        super.startService();
        logger = new LoggerImpl();
        startInfo();
    }

    @Override
    protected void stopService() {
        super.stopService();
        stopInfo();
    }

    private void startInfo() {
        startTime = System.currentTimeMillis();

        String[] banner = {"",
                " Qingzhou        |~",
                "              |/ w",
                "             / ((| \\",
                "            /((/ |)|\\",
                "  ____     ((/  (| | )  ,",
                " |----\\   (/ |  /| |'\\ /^;",
                "\\---*---Y--+-----+---+--/(",
                " \\------*---*--*---*--/",
                "  '~~ ~~~~~~~~~~~~~~~",
                ""};
        for (String line : banner) {
            logger.info(line);
        }
    }

    private void stopInfo() {
        long stopTime = System.currentTimeMillis();
        String time = calculateTimeDifference(startTime, stopTime);
        logger.info("Qingzhou has been successfully stopped, duration of this runtime: " + time);
    }


    /**
     * 计算两个时间差（年，月，星期，日，时，分，秒）
     */
    private static String calculateTimeDifference(long startTimeMillis, long endTimeMillis) {
        Date startDate = new Date();
        startDate.setTime(startTimeMillis);
        Date ednDate = new Date();
        ednDate.setTime(endTimeMillis);
        return calculateTimeDifference(startDate, ednDate);
    }

    /**
     * 计算两个时间差（年，月，星期，日，时，分，秒）
     */
    private static String calculateTimeDifference(Date startDate, Date endDate) {
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
}