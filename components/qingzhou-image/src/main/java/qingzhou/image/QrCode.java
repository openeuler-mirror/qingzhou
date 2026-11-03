package qingzhou.image;

import java.io.IOException;

public interface QrCode {
    byte[] genImage(String text) throws IOException;

    // 终端文本图形：ANSI 黑底白块与半块字符拼合，供命令行直接输出给手机扫描
    String genText(String text);
}
