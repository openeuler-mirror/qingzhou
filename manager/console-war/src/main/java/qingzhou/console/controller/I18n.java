package qingzhou.console.controller;

import qingzhou.api.Lang;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.ViewManager;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.I18nTool;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

public class I18n implements Filter<SystemControllerContext> {
    public static final String LANG_SWITCH_URI = "/lang";

    private static final I18nTool KEY_I18N = new I18nTool();
    private static final Lang DEFAULT_LANG = Lang.zh;// 这样一来，命令行和rest默认就是中文了（也可通过 --lang 参数来修改），控制台除外（有特殊处理）
    private static final ThreadLocal<Lang> SESSION_LANG = ThreadLocal.withInitial(() -> DEFAULT_LANG);// 直接修改语言
    private static final String SESSION_LANG_FLAG = "lang";// 向下兼容，不可修改
    private static final String lastUriKey = "lastUriKey";

    static {
        addKeyI18n("page.index", new String[]{" QingZhou 平台", "en:QingZhou Platform"});
        addKeyI18n("page.index.centralized", new String[]{"集中管理", "en:Centralized Management"});
        addKeyI18n("page.localInstance", new String[]{"默认实例", "en:Default Instance"});
        addKeyI18n("page.action", new String[]{"操作", "en:Action"});
        addKeyI18n("page.filter", new String[]{"搜索", "en:Search"});
        addKeyI18n("page.status", new String[]{"状态", "en:Status"});
        addKeyI18n("page.msg", new String[]{"消息", "en:Message"});
        addKeyI18n("page.browser.outdated", new String[]{"您正在使用过时的浏览器，当前页面不能支持，请升级或更换浏览器!", "en:You are using an outdated browser, the current page is not supported, please upgrade or change your browser!"});
        addKeyI18n("page.confirm", new String[]{"确定", "en:Confirm"});
        addKeyI18n("page.return", new String[]{"返回", "en:Return"});
        addKeyI18n("page.confirm.title", new String[]{"请确认", "en:Please confirm"});
        addKeyI18n("page.operationConfirm", new String[]{"是否%s此%s", "en:Whether to %s this %s"});
        addKeyI18n("page.document", new String[]{"手册", "en:Manual"});
        addKeyI18n("page.invalidate", new String[]{"注销", "en:Logout"});
        addKeyI18n("page.error", new String[]{"请求服务器出现错误，请查看服务器日志以了解详情", "en:There was an error requesting the server, please check the server log for details"});
        addKeyI18n("msg.success", new String[]{"成功", "en:Success"});
        addKeyI18n("msg.fail", new String[]{"失败", "en:Failed"});
        addKeyI18n("page.selectfile", new String[]{"选择文件", "en:Select file"});
        addKeyI18n("page.list.order", new String[]{"序号", "en:No."});
        addKeyI18n("page.copyright", new String[]{"版权所有 © 2023 保留一切权利", "en:Copyright © 2023. All rights reserved."});
        addKeyI18n("page.userlogin", new String[]{"用户登录", "en:User Login"});
        addKeyI18n("page.login", new String[]{"登录", "en:Login"});
        addKeyI18n("page.relogin", new String[]{"重新登录", "en:Re Login"});
        addKeyI18n("page.vercode", new String[]{"验证码", "en:Ver Code"});
        addKeyI18n("page.none", new String[]{"未查询到数据", "en:No data found"});
        addKeyI18n("page.login.need", new String[]{"用户未登录或会话已超时，请重新登录", "en:User is not logged in or the session has timed out, please log in again"});

        addKeyI18n("page.go", new String[]{"详情请看：", "en:For details, please see: "});
        addKeyI18n("page.gotit", new String[]{"知道了", "en:Okay, got it"});
        addKeyI18n("page.thememode", new String[]{"主题模式", "en:Theme Mode"});
        addKeyI18n("page.lang.switch.confirm", new String[]{"切换语言后，所有已打开的页面将会强制刷新，请确认已保存了相关工作状态", "en:After switching the language, all open pages will be forced to refresh, please confirm that the relevant work status has been saved"});
        addKeyI18n("page.logout.confirm", new String[]{"确定要退出当前用户", "en:Are you sure to exit the current user"});
        addKeyI18n("page.download.log.tip", new String[]{"请选择需要下载的文件", "en:Please select the file to download"});
        addKeyI18n("page.download.checkall", new String[]{"全选", "en:Check all"});
        addKeyI18n("page.download.tasktip", new String[]{"开始下载", "en:Start downloading"});
        addKeyI18n("page.layertitle.otp", new String[]{"请使用TOTP客户端扫描二维码", "en:Please use the TOTP client to scan the QR code"});
        addKeyI18n("page.placeholder.otp", new String[]{"输入扫描得到的密码", "en:Enter scanned otp"});
        addKeyI18n("page.bindsuccess.otp", new String[]{"绑定成功", "en:Bind success"});
        addKeyI18n("page.bindfail.otp", new String[]{"密码不匹配", "en:Mismatch"});
        addKeyI18n("page.info.otp", new String[]{"动态密码，选填", "en:OTP, optional"});
        addKeyI18n("page.error.network", new String[]{"服务器连接错误，请确认服务器已启动或检查网络是否通畅", "en:Server connection error, please confirm that the server has been started or check whether the network is smooth"});
        addKeyI18n("page.info.add", new String[]{"添加", "en:Add"});
        addKeyI18n("page.info.kv.name", new String[]{"变量名", "en:Name"});
        addKeyI18n("page.info.kv.value", new String[]{"值", "en:Value"});
        addKeyI18n("page.password.changed", new String[]{"密码修改成功，请重新登录", "en:Password changed successfully, please login again"});
        addKeyI18n("page.lang.switch", new String[]{"切换语言", "en:Switch The Language"});
        addKeyI18n("page.login.user", new String[]{"账户名称", "en:User"});
        addKeyI18n("page.login.password", new String[]{"账户密码", "en:Password"});
    }

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;
        HttpSession s;
        // 如果设置了中文，可以使得命令行的登录错误返回指定的i18n信息，否则默认是英文的
        I18n.setI18nLang(request, null);

        s = request.getSession(false);
        if (s == null) {
            return true;
        }

        String checkPath = RESTController.getReqUri(request);
        if (checkPath.startsWith(LANG_SWITCH_URI + "/")) {
            Lang lang = null;
            for (Lang l : Lang.values()) {
                if (checkPath.equalsIgnoreCase(LANG_SWITCH_URI + "/" + l.name())) {
                    lang = l;
                    break;
                }
            }

            if (lang != null) {
                s.setAttribute(SESSION_LANG_FLAG, lang);

                String lastUri = (String) s.getAttribute(lastUriKey);
                if (Utils.isBlank(lastUri)) {
                    lastUri = request.getContextPath() + LoginManager.INDEX_PATH;
                }
                response.sendRedirect(RESTController.encodeURL(response, lastUri)); // to welcome page
            }

            return false;
        }

        Lang lang = (Lang) s.getAttribute(SESSION_LANG_FLAG);
        if (lang != null) {
            I18n.setI18nLang(request, lang);
        }

        return true;
    }

    @Override
    public void afterFilter(SystemControllerContext context) {
        I18n.resetI18nLang();
        try {
            String requestURI = context.req.getRequestURI();
            if (requestURI.contains(DeployerConstants.REST_PREFIX + "/" + ViewManager.htmlView)) {
                // 如果没有这个判断，在查看折线图页面，发送的最后请求是 json数据，就会跳转错误
                HttpSession s = context.req.getSession(false);
                if (s != null) {
                    s.setAttribute(lastUriKey, requestURI);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void setI18nLang(HttpServletRequest request, Lang lang) {
        try {
            String p = request.getParameter(SESSION_LANG_FLAG);
            if (p != null) {
                lang = Lang.valueOf(p);
            }
        } catch (Exception ignored) {
        }

        if (lang != null) {
            SESSION_LANG.set(lang);
        }
    }

    public static Lang getI18nLang() {
        return SESSION_LANG.get();
    }

    public static void resetI18nLang() {
        SESSION_LANG.set(DEFAULT_LANG);
    }

    public static boolean isZH() {
        Lang currentLang = getI18nLang();
        return currentLang == Lang.zh || currentLang == Lang.tr;
    }

    public static String getModelI18n(String appName, String i18nKey) {
        AppInfo appInfo = SystemController.getAppInfo(appName);

        int fieldInfo = i18nKey.indexOf("model.field.info.");
        if (fieldInfo > -1) {
            String[] split = i18nKey.substring(fieldInfo).split("\\.");
            ModelInfo modelInfo = appInfo.getModelInfo(split[3]);
            ModelFieldInfo modelFieldInfo = modelInfo.getModelFieldInfo(split[4]);
            return getStringI18n(modelFieldInfo.getInfo());
        }

        int field = i18nKey.indexOf("model.field.");
        if (field > -1) {
            String[] split = i18nKey.substring(field).split("\\.");
            ModelInfo modelInfo = appInfo.getModelInfo(split[2]);
            ModelFieldInfo modelFieldInfo = modelInfo.getModelFieldInfo(split[3]);
            return getStringI18n(modelFieldInfo.getName());
        }

        int actionInfo = i18nKey.indexOf("model.action.info.");
        if (actionInfo > -1) {
            String[] split = i18nKey.substring(actionInfo).split("\\.");
            ModelInfo modelInfo = appInfo.getModelInfo(split[3]);
            ModelActionInfo modelActionInfo = modelInfo.getModelActionInfo(split[4]);
            return getStringI18n(modelActionInfo.getInfo());
        }

        int action = i18nKey.indexOf("model.action.");
        if (action > -1) {
            String[] split = i18nKey.substring(action).split("\\.");
            ModelInfo modelInfo = appInfo.getModelInfo(split[2]);
            ModelActionInfo modelActionInfo = modelInfo.getModelActionInfo(split[3]);
            return getStringI18n(modelActionInfo.getName());
        }

        int info = i18nKey.indexOf("model.info.");
        if (info > -1) {
            String[] split = i18nKey.substring(info).split("\\.");
            ModelInfo modelInfo = appInfo.getModelInfo(split[2]);
            return getStringI18n(modelInfo.getInfo());
        }

        int model = i18nKey.indexOf("model.");
        if (model > -1) {
            String modelName = i18nKey.substring(model).split("\\.")[1];
            ModelInfo modelInfo = appInfo.getModelInfo(modelName);
            if (modelInfo != null) {
                return getStringI18n(modelInfo.getName());
            }
            MenuInfo menuInfo = appInfo.getMenuInfo(modelName);
            if (menuInfo != null) {
                return getStringI18n(menuInfo.getI18n());
            }
        }

        throw new IllegalArgumentException("appName: " + appName + ", i18nKey: " + i18nKey);
    }

    public static String getStringI18n(String[] i18n) {
        Map<Lang, String> i18nMap = I18nTool.retrieveI18n(i18n);

        return i18nMap.get(getI18nLang());
    }

    public static void addKeyI18n(String key, String[] i18n) {
        KEY_I18N.addI18n(key, i18n, true);
    }

    public static String getKeyI18n(String key, Object... args) {
        return KEY_I18N.getI18n(getI18nLang(), key, args);
    }
}
