package qingzhou.console.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SafeCheckerUtil {
    private static final String[] CommandInjectionRisk = new String[]{"`", "$", ";", "&", "|", "{", "}", "(", ")", "[", "]", "../", "..\\", "*", "%", "~", "^", "!"};// windows 路径会存在空格

    public static String hasCommandInjectionRiskWithSkip(String arg, String skips) {
        OUT:
        for (String f : CommandInjectionRisk) { // 命令行执行注入漏洞
            if (skips != null) {
                if (skips.contains(f)) {
                    continue OUT;
                }
            }
            if (arg.contains(f)) {
                return f;
            }
        }

        return null;
    }

    public static boolean hasCmdInjectionRisk(String arg) {
        for (String f : new String[]{"`"}) {// 命令行执行注入漏洞
            if (arg.contains(f)) {
                return true;
            }
        }

        return false;
    }

    private static final Pattern scriptPattern1 = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);

    public static boolean checkIsXSS(String check) {
        return !checkXssOk(check);
    }

    // Level1 的检查，可以让大多数的正则（允许使用括号、中括号等）通过
    public static boolean checkXssLevel1(String check) {
        if (StringUtil.isBlank(check)) {
            return true;
        }

        //判断url是否带有<>
        String resultUrl = check.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        if (!resultUrl.equals(check)) {
            return false;
        }

        resultUrl = resultUrl.replaceAll("eval\\((.*)\\)", "");
        if (!resultUrl.equals(check)) {
            return false;
        }

        //onmouseover漏洞
        List<String> onXXEventPrefixList = new ArrayList<String>();
        onXXEventPrefixList.addAll(Arrays.asList(new String[]{"%20", "&nbsp;", "\"", "'", "/", "\\+"}));
        resultUrl = scriptPattern1.matcher(resultUrl).replaceAll("");
        if (!resultUrl.equals(check)) {
            return false;
        }

        // 拦截这种攻击方式：payload:'onmousemove         =confirm(1)//
        if ((resultUrl.contains("'") || resultUrl.contains("\""))
                && resultUrl.indexOf(")") > resultUrl.indexOf("(")) {
            return false;
        }


        return true;
    }

    public static boolean checkXssOk(String check) {
        if (StringUtil.isBlank(check)) return true;

        if (!checkXssLevel1(check)) {
            return false;
        }

        String resultUrl = check.replaceAll("\\(", "&#40").replaceAll("\\)", "&#41");
        if (!resultUrl.equals(check)) {
            return false;
        }


        resultUrl = resultUrl.replaceAll("\\[", "&#91").replaceAll("\\]", "&#93");
        if (!resultUrl.equals(check)) {
            return false;
        }

        return true;
    }

    private SafeCheckerUtil() {
    }
}
