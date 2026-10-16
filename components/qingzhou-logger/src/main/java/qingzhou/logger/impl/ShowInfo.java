package qingzhou.logger.impl;

import java.time.Duration;

import qingzhou.logger.Logger;

class ShowInfo {
    private final Logger logger;

    private long startTime;

    ShowInfo(Logger logger) {
        this.logger = logger;
    }

    void activate() {
        startTime = System.currentTimeMillis();

        String[] banner = {
                "",
                " Qingzhou        |~",
                "              |/ w",
                "             / ((| \\",
                "            /((/ |)|\\",
                "  ____     ((/  (| | )  ,",
                " |----\\   (/ |  /| |'\\ /^;",
                "\\---*---Y--+-----+---+--/(",
                " \\------*---*--*---*--/",
                "  '~~ ~~~~~~~~~~~~~~~"
        };
        for (String line : banner) {
            logger.info(line);
        }
    }

    void deactivate() {
        long stopTime = System.currentTimeMillis();
        String time = calculateTimeDifference(startTime, stopTime);
        logger.info("Qingzhou has been successfully stopped, duration of this runtime: " + time);
    }

    /**
     * 运行总时长展示：按日/时/分/秒拆分（仅用于停机日志，无需日历精度的年月拆解）
     */
    private static String calculateTimeDifference(long startTimeMillis, long endTimeMillis) {
        Duration duration = Duration.ofMillis(Math.max(0, endTimeMillis - startTimeMillis));
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        return (days == 0 ? "" : days + " days ")
                + (hours == 0 ? "" : hours + " hours ")
                + (minutes == 0 ? "" : minutes + " minutes ")
                + (seconds == 0 ? "" : seconds + " seconds");
    }
}
