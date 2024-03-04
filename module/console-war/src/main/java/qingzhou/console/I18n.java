package qingzhou.console;

import qingzhou.api.Lang;
import qingzhou.console.controller.SetI18n;
import qingzhou.framework.app.I18nTool;
import qingzhou.framework.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class I18n {
    public static final Lang DEFAULT_LANG = Lang.zh;// 这样一来，命令行和rest默认就是中文了（也可通过 --lang 参数来修改），控制台除外（有特殊处理）

    private static final ThreadLocal<Lang> I18n_Lang = ThreadLocal.withInitial(() -> DEFAULT_LANG);// 直接修改语言

    private I18n() {
    }

    public static void setI18nLang(HttpServletRequest request, Lang lang) {
        try {
            String p = request.getParameter(SetI18n.SESSION_LANG_FLAG);
            if (StringUtil.notBlank(p)) {
                lang = Lang.valueOf(p);
            }
        } catch (Exception ignored) {
        }

        if (lang != null) {
            I18n_Lang.set(lang);
        }
    }

    public static void resetI18nLang() {
        I18n_Lang.set(DEFAULT_LANG);
    }

    /**
     * 返回生效的 I18n Lang
     */
    public static Lang getI18nLang() {
        return I18n_Lang.get();
    }

    public static boolean isZH() {
        Lang currentLang = getI18nLang();
        return currentLang == Lang.zh || currentLang == Lang.tr;
    }

    public static String getString(String appName, String i18nKey) {
        AppStub appStub = ConsoleWarHelper.getAppStub(appName);
        return appStub.getI18n(I18n.getI18nLang(), i18nKey);
    }

    public static String getString(String[] i18n) {
        Map<Lang, String> i18nMap = I18nTool.retrieveI18n(i18n);

        return i18nMap.get(getI18nLang());
    }
}
