package qingzhou.llm.impl;

public class LogUtil {
    public static void println(String msg) {
        System.err.println(msg); // 方便TW等集成，不用Logger对象
    }
}
