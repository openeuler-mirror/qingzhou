package qingzhou.console.controller;

import qingzhou.api.Lang;
import qingzhou.console.ConsoleI18n;
import qingzhou.console.I18n;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.ViewManager;
import qingzhou.framework.util.pattern.Filter;
import qingzhou.framework.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static qingzhou.console.login.LoginManager.*;

public class I18nFilter implements Filter<HttpServletContext> {
    public static final String LANG_SWITCH_URI = "/lang";
    public static final String SESSION_LANG_FLAG = "lang";// 向下兼容，不可修改
    private static final String lastUriKey = "lastUriKey";

    public static void setI18nLang(HttpServletRequest request, Lang lang) {
        try {
            String p = request.getParameter(SESSION_LANG_FLAG);
            if (StringUtil.notBlank(p)) {
                lang = Lang.valueOf(p);
            }
        } catch (Exception ignored) {
        }

        if (lang != null) {
            I18n.setI18nLang(lang);
        }
    }

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;
        HttpSession s;
        // 如果设置了中文，可以使得命令行的登录错误返回指定的i18n信息，否则默认是英文的
        I18nFilter.setI18nLang(request, null);

        s = request.getSession(false);
        if (s == null) {
            return true;
        }

        String checkPath = RESTController.retrieveServletPathAndPathInfo(request);
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
                if (StringUtil.isBlank(lastUri)) {
                    lastUri = request.getContextPath() + RESTController.INDEX_PATH;
                }
                response.sendRedirect(PageBackendService.encodeURL(response, lastUri)); // to welcome page
            }

            return false;
        }

        Lang lang = (Lang) s.getAttribute(SESSION_LANG_FLAG);
        if (lang != null) {
            I18n.setI18nLang(lang);
        }

        return true;
    }

    @Override
    public void afterFilter(HttpServletContext context) {
        I18n.resetI18nLang();
        try {
            String requestURI = context.req.getRequestURI();
            if (requestURI.contains(RESTController.REST_PREFIX + "/" + ViewManager.htmlView)) {
                // 如果没有这个判断，在查看折线图页面，发送的最后请求是 json数据，就会跳转错误
                HttpSession s = context.req.getSession(false);
                s.setAttribute(lastUriKey, requestURI);
            }
        } catch (Exception ignored) {
        }
    }

    static {
        ConsoleI18n.addI18N(LOGIN_ERROR_MSG_KEY, new String[]{"登录失败，用户名或密码错误。当前登录失败 %s 次，连续失败 %s 次，账户将锁定", "en:Login failed, wrong username or password. The current login failed %s times, and the account will be locked after %s consecutive failures"});
        ConsoleI18n.addI18N(LOCKED_MSG_KEY, new String[]{"连续登录失败 %s 次，账户已经锁定，请 %s 分钟后重试", "en:Login failed %s times in a row, account is locked, please try again in %s minutes"});
        ConsoleI18n.addI18N(TWO_FA_MSG_KEY, new String[]{"双因子认证认证失败：动态密码错误", "en:Two-factor authentication authentication failed: dynamic password error"});
        ConsoleI18n.addI18N(ACCEPT_AGREEMENT_MSG_KEY, new String[]{"请确保您已阅读并同意本产品的《许可协议》", "en:Please ensure that you have read and agree to the <License Agreement> for this product"});
        ConsoleI18n.addI18N("jmx.credentials.element.isNull", new String[]{"用户名或密码不能为空", "en:The user name or password cannot be empty"});

        ConsoleI18n.addI18N("page.index", new String[]{"轻舟平台", "en:Qingzhou Platform"});
        ConsoleI18n.addI18N("page.index.centralized", new String[]{"集中管理", "en:Centralized Management"});
        ConsoleI18n.addI18N("page.localInstance", new String[]{"默认实例", "en:Default Instance"});
        ConsoleI18n.addI18N("page.action", new String[]{"操作", "en:Action"});
        ConsoleI18n.addI18N("page.filter", new String[]{"搜索", "en:Search"});
        ConsoleI18n.addI18N("page.status", new String[]{"状态", "en:Status"});
        ConsoleI18n.addI18N("page.msg", new String[]{"消息", "en:Message"});
        ConsoleI18n.addI18N("page.browser.outdated", new String[]{"您正在使用过时的浏览器，当前页面不能支持，请升级或更换浏览器!", "en:You are using an outdated browser, the current page is not supported, please upgrade or change your browser!"});
        ConsoleI18n.addI18N("page.confirm", new String[]{"确定", "en:Confirm"});
        ConsoleI18n.addI18N("page.cancel", new String[]{"返回", "en:Cancel"});
        ConsoleI18n.addI18N("page.confirm.title", new String[]{"请确认", "en:Please confirm"});
        ConsoleI18n.addI18N("page.operationConfirm", new String[]{"是否%s该%s", "en:Whether to %s this %s"});
        ConsoleI18n.addI18N("page.document", new String[]{"手册", "en:Manual"});
        ConsoleI18n.addI18N("page.invalidate", new String[]{"注销", "en:Logout"});
        ConsoleI18n.addI18N("page.error", new String[]{"请求服务器出现错误，请查看服务器日志以了解详情", "en:There was an error requesting the server, please check the server log for details"});
        ConsoleI18n.addI18N("msg.success", new String[]{"成功", "en:Success"});
        ConsoleI18n.addI18N("msg.fail", new String[]{"失败", "en:Failed"});
        ConsoleI18n.addI18N("page.selectfile", new String[]{"选择文件", "en:Select file"});
        ConsoleI18n.addI18N("page.list.order", new String[]{"序号", "en:No."});
        ConsoleI18n.addI18N("page.copyright", new String[]{"版权所有 © 2023 保留一切权利", "en:Copyright © 2023. All rights reserved."});
        ConsoleI18n.addI18N("page.userlogin", new String[]{"用户登录", "en:User Login"});
        ConsoleI18n.addI18N("page.login", new String[]{"登录", "en:Login"});
        ConsoleI18n.addI18N("page.relogin", new String[]{"重新登录", "en:Re Login"});
        ConsoleI18n.addI18N("page.vercode", new String[]{"验证码", "en:Ver Code"});
        ConsoleI18n.addI18N("page.none", new String[]{"未查询到数据", "en:No data found"});
        ConsoleI18n.addI18N("page.login.need", new String[]{"用户未登录或会话已超时，请重新登录", "en:User is not logged in or the session has timed out, please log in again"});

        ConsoleI18n.addI18N("page.go", new String[]{"详情请看：", "en:For details, please see: "});
        ConsoleI18n.addI18N("page.gotit", new String[]{"知道了", "en:Okay, got it"});
        ConsoleI18n.addI18N("page.lang.switch.confirm", new String[]{"切换语言后，所有已打开的页面将会强制刷新，请确认已保存了相关工作状态", "en:After switching the language, all open pages will be forced to refresh, please confirm that the relevant work status has been saved"});
        ConsoleI18n.addI18N("page.logout.confirm", new String[]{"确定要退出当前用户", "en:Are you sure to exit the current user"});
        ConsoleI18n.addI18N("page.download.log.tip", new String[]{"请选择需要下载的文件", "en:Please select the file to download"});
        ConsoleI18n.addI18N("page.download.checkall", new String[]{"全选", "en:Check all"});
        ConsoleI18n.addI18N("page.download.tasktip", new String[]{"开始下载", "en:Start downloading"});
        ConsoleI18n.addI18N("page.layertitle.2fa", new String[]{"扫描二维码绑定双因子认证密钥", "en:Scan the QR code to bind the two-factor authentication key"});
        ConsoleI18n.addI18N("page.placeholder.2fa", new String[]{"请扫描二维码保存后输入验证码完成绑定", "en:Please scan the QR code to save and enter the verification code to complete the binding"});
        ConsoleI18n.addI18N("page.bindsuccess.2fa", new String[]{"绑定成功", "en:Bind success"});
        ConsoleI18n.addI18N("page.bindfail.2fa", new String[]{"绑定失败", "en:Bind error"});
        ConsoleI18n.addI18N("page.info.2fa", new String[]{"双因子认证密码，选填", "en:Two-factor authentication password, optional"});
        ConsoleI18n.addI18N("page.error.network", new String[]{"服务器连接错误，请确认服务器已启动或检查网络是否通畅", "en:Server connection error, please confirm that the server has been started or check whether the network is smooth"});
        ConsoleI18n.addI18N("page.info.add", new String[]{"添加", "en:Add"});
        ConsoleI18n.addI18N("page.info.kv.name", new String[]{"变量名", "en:Name"});
        ConsoleI18n.addI18N("page.info.kv.value", new String[]{"值", "en:Value"});
        ConsoleI18n.addI18N("page.password.changed", new String[]{"密码修改成功，请重新登录", "en:Password changed successfully, please login again"});
        ConsoleI18n.addI18N("page.lang.switch", new String[]{"切换语言", "en:Switch The Language"});

        ConsoleI18n.addI18N("page.guide", new String[]{"新手引导", "en:Beginner Guide"});
        ConsoleI18n.addI18N("page.guide.previous", new String[]{"上一步", "en:Previous"});
        ConsoleI18n.addI18N("page.guide.next", new String[]{"下一步", "en:Next"});
        ConsoleI18n.addI18N("page.guide.skip", new String[]{"跳过", "en:Skip"});
        ConsoleI18n.addI18N("page.guide.finish", new String[]{"完成", "en:Finish"});

        ConsoleI18n.addI18N("page.guide.pwd", new String[]{"首次登录，请修改初始密码", "en:To log in for the first time, please change the initial password"});
        ConsoleI18n.addI18N("page.guide.help", new String[]{"查看用户手册，帮助您详细了解如何使用产品", "en:View the user manual to help you learn more about how to use products"});
        ConsoleI18n.addI18N("page.guide.home", new String[]{"点击“首页”，可查看产品的名称、版本号、命名空间、运行模式及授权等信息", "en:Click \"Home\" to view the name, version number, namespace, operating mode and authorization of the product"});
        ConsoleI18n.addI18N("page.guide.res", new String[]{"展开“资源管理”，可创建应用所需要使用的数据库、会话服务器、应用类库等资源", "en:Expand Resource Management to create resources such as databases, session servers, and application class libraries that your application needs to use"});
    }
}
