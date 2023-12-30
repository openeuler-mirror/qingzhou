package qingzhou.framework.console;

import qingzhou.framework.AppInfo;
import qingzhou.framework.impl.ConsoleContextImpl;
import qingzhou.framework.impl.FrameworkContextImpl;

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

    public static String getString(String appName, String i18nKey, Lang lang) {
        AppInfo appInfo = FrameworkContextImpl.getFrameworkContext().getAppInfoManager().getAppInfo(appName);
        ConsoleContextImpl consoleContext = (ConsoleContextImpl) appInfo.getAppContext().getConsoleContext();
        return consoleContext.getI18N(lang, i18nKey);
    }

    public static String getString(String appName, String i18nKey) {
        AppInfo appInfo = FrameworkContextImpl.getFrameworkContext().getAppInfoManager().getAppInfo(appName);
        return appInfo.getAppContext().getConsoleContext().getI18N(i18nKey);
    }

    public static String getString(String[] i18n) {
        Map<Lang, String> i18nMap = Lang.parseI18n(i18n);

        return i18nMap.get(getI18nLang());
    }
}
