package qingzhou.console.i18n;

import qingzhou.api.Lang;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.I18nTool;
import qingzhou.registry.AppInfo;
import qingzhou.registry.MenuInfo;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

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
            if (p != null) {
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
        AppInfo appInfo = SystemController.getAppInfo(appName);

        int fieldInfo = i18nKey.indexOf("model.field.info.");
        if (fieldInfo > -1) {
            String[] split = i18nKey.substring(fieldInfo).split("\\.");
            ModelInfo modelInfo = appInfo.getModelInfo(split[3]);
            ModelFieldInfo modelFieldInfo = modelInfo.getModelFieldInfo(split[4]);
            return getString(modelFieldInfo.getInfo());
        }

        int field = i18nKey.indexOf("model.field.");
        if (field > -1) {
            String[] split = i18nKey.substring(field).split("\\.");
            ModelInfo modelInfo = appInfo.getModelInfo(split[2]);
            ModelFieldInfo modelFieldInfo = modelInfo.getModelFieldInfo(split[3]);
            return getString(modelFieldInfo.getName());
        }

        int actionInfo = i18nKey.indexOf("model.action.info.");
        if (actionInfo > -1) {
            String[] split = i18nKey.substring(actionInfo).split("\\.");
            ModelInfo modelInfo = appInfo.getModelInfo(split[3]);
            ModelActionInfo modelActionInfo = modelInfo.getModelActionInfo(split[4]);
            return getString(modelActionInfo.getInfo());
        }

        int action = i18nKey.indexOf("model.action.");
        if (action > -1) {
            String[] split = i18nKey.substring(action).split("\\.");
            ModelInfo modelInfo = appInfo.getModelInfo(split[2]);
            ModelActionInfo modelActionInfo = modelInfo.getModelActionInfo(split[3]);
            return getString(modelActionInfo.getName());
        }

        int info = i18nKey.indexOf("model.info.");
        if (info > -1) {
            String[] split = i18nKey.substring(info).split("\\.");
            ModelInfo modelInfo = appInfo.getModelInfo(split[2]);
            return getString(modelInfo.getInfo());
        }

        int model = i18nKey.indexOf("model.");
        if (model > -1) {
            String modelName = i18nKey.substring(model).split("\\.")[1];
            ModelInfo modelInfo = appInfo.getModelInfo(modelName);
            if (modelInfo != null) {
                return getString(modelInfo.getName());
            }
            MenuInfo menuInfo = appInfo.getMenuInfo(modelName);
            if (menuInfo != null) {
                return getString(menuInfo.getI18n());
            }
        }

        throw new IllegalArgumentException("appName: " + appName + ", i18nKey: " + i18nKey);
    }

    public static String getString(String[] i18n) {
        Map<Lang, String> i18nMap = I18nTool.retrieveI18n(i18n);

        return i18nMap.get(getI18nLang());
    }
}
