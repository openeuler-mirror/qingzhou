package qingzhou.logger.impl;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import qingzhou.logger.Logger;

public class LoggerImpl implements Logger {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public void info(String msg) {
        log("INFO", msg, null);
    }

    @Override
    public void info(String msg, Throwable e) {
        log("INFO", msg, e);
    }

    @Override
    public void warn(String msg) {
        log("WARN", msg, null);
    }

    @Override
    public void warn(String msg, Throwable e) {
        log("WARN", msg, e);
    }

    @Override
    public void error(String msg) {
        log("ERROR", msg, null);
    }

    @Override
    public void error(String msg, Throwable e) {
        log("ERROR", msg, e);
    }

    private void log(String level, String msg, Throwable e) {
        PrintStream out = "ERROR".equals(level) || "WARN".equals(level) ? System.err : System.out;
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(SDF.format(new Date())).append("]");
        sb.append(" [").append(level).append("]");
        sb.append(" [QingzhouAgent] ");
        sb.append(msg);
        out.println(sb.toString());
        if (e != null) {
            e.printStackTrace(out);
        }
    }
}