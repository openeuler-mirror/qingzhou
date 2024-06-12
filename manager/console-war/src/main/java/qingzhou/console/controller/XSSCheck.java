package qingzhou.console.controller;

import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class XSSCheck implements Filter<HttpServletContext> {
    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        if (!check(context.req)) {
            context.resp.sendError(HttpServletResponse.SC_FORBIDDEN, "XSS FORBIDDEN");
            return false;
        }

        return true;
    }

    /**
     * 判断url参数是否符合以下规则
     */
    private boolean check(HttpServletRequest request) {
        //获取请求url
        String inputUrl = request.getQueryString();
        if (inputUrl != null) {
            try {
                inputUrl = URLDecoder.decode(inputUrl, "utf-8");
            } catch (UnsupportedEncodingException ignored) {
            }
        } else {
            inputUrl = request.getPathInfo();
        }

        if (XSSChecker.checkIsXSS(inputUrl)) return false;

        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap != null) {
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                if (XSSChecker.checkIsXSS(key)) return false;
                String[] value = entry.getValue();
                if (value != null) {
                    for (String s : value) {
                        if (XSSChecker.checkIsXSS(s)) return false;
                    }
                }
            }
        }

        return true;
    }

    static class XSSChecker {
        private static final Pattern scriptPattern1 = Pattern.compile("vbscript:", 2);

        public XSSChecker() {
        }

        public static boolean checkIsXSS(String check) {
            return !checkXssOk(check);
        }

        public static boolean checkXssLevel1(String check) {
            if (check == null || check.trim().isEmpty()) {
                return true;
            } else {
                String resultUrl = check.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                if (!resultUrl.equals(check)) {
                    return false;
                } else {
                    resultUrl = resultUrl.replaceAll("eval\\((.*)\\)", "");
                    if (!resultUrl.equals(check)) {
                        return false;
                    } else {
                        List<String> onXXEventPrefixList = new ArrayList();
                        onXXEventPrefixList.addAll(Arrays.asList("%20", "&nbsp;", "\"", "'", "/", "\\+"));
                        resultUrl = scriptPattern1.matcher(resultUrl).replaceAll("");
                        if (!resultUrl.equals(check)) {
                            return false;
                        } else {
                            return !resultUrl.contains("'") && !resultUrl.contains("\"") || resultUrl.indexOf(")") <= resultUrl.indexOf("(");
                        }
                    }
                }
            }
        }

        public static boolean checkXssOk(String check) {
            if (check == null || check.trim().isEmpty()) {
                return true;
            } else if (!checkXssLevel1(check)) {
                return false;
            } else {
                String resultUrl = check.replaceAll("\\(", "&#40").replaceAll("\\)", "&#41");
                if (!resultUrl.equals(check)) {
                    return false;
                } else {
                    resultUrl = resultUrl.replaceAll("\\[", "&#91").replaceAll("\\]", "&#93");
                    return resultUrl.equals(check);
                }
            }
        }
    }
}
