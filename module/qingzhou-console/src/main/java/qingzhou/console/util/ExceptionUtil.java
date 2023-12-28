package qingzhou.console.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ExceptionUtil {
    public static RuntimeException unexpectedException(Throwable t) {
        return new IllegalStateException(t);
    }

    public static RuntimeException unexpectedException(String msg) {
        return new IllegalStateException(msg);
    }

    public static RuntimeException unexpectedException() {
        return new IllegalStateException("Unexpected Exception !!!");
    }

    public static String stackTrace(Throwable t) {
        if (t == null) {
            return "";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(bos);
        t.printStackTrace(stream);
        stream.flush();
        return bos.toString();
    }

    private ExceptionUtil() {
    }
}
