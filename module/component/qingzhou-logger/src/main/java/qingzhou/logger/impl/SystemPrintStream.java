package qingzhou.logger.impl;


import qingzhou.logger.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Formatter;
import java.util.Locale;

class SystemPrintStream extends PrintStream {
    private final ThreadLocal<Formatter> FORMATTER_THREAD_LOCAL = ThreadLocal.withInitial(() -> new Formatter(new StringBuilder()));

    private final Logger logger;
    private final boolean useWarnLevel;

    SystemPrintStream(Logger logger, boolean useWarnLevel) {
        super(new OutputStream() {
            @Override
            public void write(int b) {
            }
        });

        this.logger = logger;
        this.useWarnLevel = useWarnLevel;
    }

    private void logMsg(String msg) {
        if (useWarnLevel) {
            logger.warn(msg);
        } else {
            logger.info(msg);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean checkError() {
        return false;
    }

    @Override
    protected void setError() {
    }

    @Override
    protected void clearError() {
    }

    @Override
    public void write(int b) {
        logMsg(String.valueOf(b));
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        if (buf == null) {
            return;
        }
        logMsg(new String(buf, off, len));
    }

    @Override
    public void print(boolean b) {
        logMsg(String.valueOf(b));
    }

    @Override
    public void print(char c) {
        logMsg(String.valueOf(c));
    }

    @Override
    public void print(int i) {
        logMsg(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        logMsg(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        logMsg(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        logMsg(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        if (s == null) {
            return;
        }
        logMsg(new String(s));
    }

    @Override
    public void print(String s) {
        logMsg(s);
    }

    @Override
    public void print(Object obj) {
        logMsg(String.valueOf(obj));
    }

    @Override
    public void println() {
        logMsg("");
    }

    @Override
    public void println(boolean x) {
        logMsg(String.valueOf(x));
    }

    @Override
    public void println(char x) {
        logMsg(String.valueOf(x));
    }

    @Override
    public void println(int x) {
        logMsg(String.valueOf(x));
    }

    @Override
    public void println(long x) {
        logMsg(String.valueOf(x));
    }

    @Override
    public void println(float x) {
        logMsg(String.valueOf(x));
    }

    @Override
    public void println(double x) {
        logMsg(String.valueOf(x));
    }

    @Override
    public void println(char[] x) {
        if (x == null) {
            return;
        }
        logMsg(String.valueOf(x));
    }

    @Override
    public void println(String x) {
        logMsg(x);
    }

    @Override
    public void println(Object x) {
        logMsg(String.valueOf(x));
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        return format(format, args);
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        return format(format, args);
    }

    @Override
    public PrintStream format(String format, Object... args) {
        if (format != null) {
            Formatter formatter = FORMATTER_THREAD_LOCAL.get();
            formatter.format(format, args);
            StringBuilder out = (StringBuilder) formatter.out();
            String x = out.toString();
            out.setLength(0);
            logMsg(x);
        }
        return this;
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        return format(format, args);
    }

    @Override
    public PrintStream append(CharSequence csq) {
        logMsg(String.valueOf(csq));
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        CharSequence cs = (csq == null ? "null" : csq);
        logMsg(cs.subSequence(start, end).toString());
        return this;
    }

    @Override
    public PrintStream append(char c) {
        logMsg(String.valueOf(c));
        return this;
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b == null) {
            return;
        }
        logMsg(new String(b));
    }
}
