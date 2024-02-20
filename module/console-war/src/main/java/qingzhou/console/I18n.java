package qingzhou.console;

import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.framework.app.I18NStore;
import qingzhou.framework.api.Lang;

import java.util.Map;

public class I18n {
    public static final Lang DEFAULT_LANG = Lang.zh;// 这样一来，命令行和rest默认就是中文了（也可通过 --lang 参数来修改），控制台除外（有特殊处理）

    private static final ThreadLocal<Lang> I18n_Lang = new ThreadLocal<>();// 直接修改语言

    private I18n() {
    }

    public static void setI18nLang(Lang lang) {
        I18n_Lang.set(lang);
    }

    public static void resetI18nLang() {
        I18n_Lang.remove();
    }

    /**
     * 返回生效的 I18n Lang
     */
    public static Lang getI18nLang() {
        Lang lang = I18n_Lang.get();
        if (lang != null) {
            return lang;
        }
        return DEFAULT_LANG;
    }

    public static boolean isZH() {
        Lang currentLang = getI18nLang();
        return currentLang == Lang.zh || currentLang == Lang.tr;
    }

    public static String getString(String appName, String i18nKey) {
        AppStub appStub = ConsoleWarHelper.getAppStub(appName);
        return appStub.getI18N(I18n.getI18nLang(), i18nKey);
    }

    public static String getString(String[] i18n) {
        Map<Lang, String> i18nMap = I18NStore.retrieveI18n(i18n);

        return i18nMap.get(getI18nLang());
    }
}
