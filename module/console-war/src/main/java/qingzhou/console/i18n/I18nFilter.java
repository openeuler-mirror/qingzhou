package qingzhou.console.i18n;

import qingzhou.api.console.ConsoleContext;
import qingzhou.console.ConsoleUtil;
import qingzhou.console.controller.HttpServletContext;
import qingzhou.console.controller.RESTController;
import qingzhou.console.util.StringUtil;
import qingzhou.console.view.ViewManager;
import qingzhou.framework.app.I18n;
import qingzhou.framework.app.Lang;
import qingzhou.framework.pattern.Filter;

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

        String checkPath = ConsoleUtil.retrieveServletPathAndPathInfo(request);
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
                response.sendRedirect(ConsoleUtil.encodeRedirectURL(request, response, lastUri)); // to welcome page
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
        ConsoleContext master = ConsoleUtil.getAppContext(null).getConsoleContext();// todo

        master.addI18N(LOGIN_ERROR_MSG_KEY, new String[]{"登录失败，用户名或密码错误。当前登录失败 %s 次，连续失败 %s 次，账户将锁定", "en:Login failed, wrong username or password. The current login failed %s times, and the account will be locked after %s consecutive failures"});
        master.addI18N(LOCKED_MSG_KEY, new String[]{"连续登录失败 %s 次，账户已经锁定，请 %s 分钟后重试", "en:Login failed %s times in a row, account is locked, please try again in %s minutes"});
        master.addI18N(TWO_FA_MSG_KEY, new String[]{"双因子认证认证失败：动态密码错误", "en:Two-factor authentication authentication failed: dynamic password error"});
        master.addI18N(ACCEPT_AGREEMENT_MSG_KEY_MISSING, new String[]{"请输入同意本产品《许可协议》的参数：" + LOGIN_ACCEPT_AGREEMENT + "=true", "en:Please enter the parameters of agreeing to the License Agreement of this product:" + LOGIN_ACCEPT_AGREEMENT + "=true"});
        master.addI18N(ACCEPT_AGREEMENT_MSG_KEY, new String[]{"请确保您已阅读并同意本产品的《许可协议》", "en:Please ensure that you have read and agree to the <License Agreement> for this product"});
        master.addI18N("jmx.credentials.miss", new String[]{"请输入身份认证信息", "en:Please enter authentication information"});
        master.addI18N("jmx.credentials.type.error", new String[]{"认证信息应为字符串数组类型，检测到不合法数据：%s", "en:Authentication information should be of type string array, invalid data detected: %s"});
        master.addI18N("jmx.credentials.element.error", new String[]{"认证信息不完整，即字符串数组个数不足够", "en:The authentication information is incomplete, that is, the number of string arrays is insufficient"});
        master.addI18N("jmx.credentials.element.isNull", new String[]{"用户名或密码不能为空", "en:The user name or password cannot be empty"});
        master.addI18N("jmx.authentication.invalid", new String[]{"JMX 认证无效，需使用用户登录认证", "en:JMX authentication is invalid, use user login authentication"});

        master.addI18N("page.index", new String[]{"管理控制台", "en:Console"});
        master.addI18N("page.index.Favorites", new String[]{"我的收藏", "en:My Favorites"});
        master.addI18N("page.index.service", new String[]{"基础配置", "en:Basic Config"});
        master.addI18N("page.index.system", new String[]{"系统管理", "en:System Management"});
        master.addI18N("page.index.health", new String[]{"监视管理", "en:Monitor Management"});
        master.addI18N("page.index.diagnostic", new String[]{"诊断管理", "en:Healthy Management"});
        master.addI18N("page.index.support", new String[]{"扩展服务", "en:Extended Service"});
        master.addI18N("page.index.centralized", new String[]{"集中管理", "en:Centralized Management"});
        master.addI18N("page.index.log", new String[]{"日志管理", "en:Log Management"});
        master.addI18N("page.localInstance", new String[]{"默认实例", "en:Default Instance"});
        master.addI18N("page.index.console", new String[]{"安全配置", "en:Security Config"});
        master.addI18N("page.action", new String[]{"操作", "en:Action"});
        master.addI18N("page.filter", new String[]{"搜索", "en:Search"});
        master.addI18N("page.status", new String[]{"状态", "en:Status"});
        master.addI18N("page.msg", new String[]{"消息", "en:Message"});
        master.addI18N("page.browser.outdated", new String[]{"您正在使用过时的浏览器，当前页面不能支持，请升级或更换浏览器!", "en:You are using an outdated browser, the current page is not supported, please upgrade or change your browser!"});
        master.addI18N("page.confirm", new String[]{"确定", "en:Confirm"});
        master.addI18N("page.cancel", new String[]{"返回", "en:Cancel"});
        master.addI18N("page.confirm.title", new String[]{"请确认", "en:Please confirm"});
        master.addI18N("page.operationConfirm", new String[]{"是否%s该%s", "en:Whether to %s this %s"});
        master.addI18N("page.action.confirm", new String[]{"是否%s", "en:Whether to %s"});
        master.addI18N("page.document", new String[]{"手册", "en:Manual"});
        master.addI18N("page.invalidate", new String[]{"注销", "en:Logout"});
        master.addI18N("page.error", new String[]{"请求服务器出现错误，请查看服务器日志以了解详情", "en:There was an error requesting the server, please check the server log for details"});
        master.addI18N("msg.success", new String[]{"成功", "en:Success"});
        master.addI18N("msg.fail", new String[]{"失败", "en:Failed"});
        master.addI18N("fileFrom.upload", new String[]{"上传文件", "en:Upload File"});
        master.addI18N("fileFrom.server", new String[]{"服务器文件", "en:Server File"});
        master.addI18N("reportmode.pull", new String[]{"拉取", "en:pull"});
        master.addI18N("reportmode.push", new String[]{"推送", "en:push"});
        master.addI18N("page.selectfile", new String[]{"选择文件", "en:Select file"});
        master.addI18N("page.list.order", new String[]{"序号", "en:No."});
        master.addI18N("page.copyright", new String[]{"版权所有 © 2023 openEuler 保留一切权利", "en:Copyright © 2023 openEuler. All rights reserved."});
        master.addI18N("page.userlogin", new String[]{"用户登录", "en:User Login"});
        master.addI18N("page.login", new String[]{"登录", "en:Login"});
        master.addI18N("page.relogin", new String[]{"重新登录", "en:Re Login"});
        master.addI18N("page.vercode", new String[]{"验证码", "en:Ver Code"});
        master.addI18N("page.none", new String[]{"未查询到数据", "en:No data found"});
        master.addI18N("page.login.need", new String[]{"用户未登录或会话已超时，请重新登录", "en:User is not logged in or the session has timed out, please log in again"});

        master.addI18N("page.go", new String[]{"详情请看：", "en:For details, please see: "});
        master.addI18N("page.gotit", new String[]{"知道了", "en:Okay, got it"});
        master.addI18N("page.lang.switch.confirm", new String[]{"切换语言后，所有已打开的页面将会强制刷新，请确认已保存了相关工作状态", "en:After switching the language, all open pages will be forced to refresh, please confirm that the relevant work status has been saved"});
        master.addI18N("page.logout.confirm", new String[]{"确定要退出当前用户", "en:Are you sure to exit the current user"});
        master.addI18N("page.download.log.tip", new String[]{"请选择需要下载的文件", "en:Please select the file to download"});
        master.addI18N("page.download.checkall", new String[]{"全选", "en:Check all"});
        master.addI18N("page.download.tasktip", new String[]{"开始下载", "en:Start downloading"});
        master.addI18N("page.layertitle.2fa", new String[]{"扫描二维码绑定双因子认证密钥", "en:Scan the QR code to bind the two-factor authentication key"});
        master.addI18N("page.placeholder.2fa", new String[]{"请扫描二维码保存后输入验证码完成绑定", "en:Please scan the QR code to save and enter the verification code to complete the binding"});
        master.addI18N("page.bindsuccess.2fa", new String[]{"绑定成功", "en:Bind success"});
        master.addI18N("page.bindfail.2fa", new String[]{"绑定失败", "en:Bind error"});
        master.addI18N("page.info.2fa", new String[]{"双因子认证密码，选填", "en:Two-factor authentication password, optional"});
        master.addI18N("page.error.network", new String[]{"服务器连接错误，请确认服务器已启动或检查网络是否通畅", "en:Server connection error, please confirm that the server has been started or check whether the network is smooth"});
        master.addI18N("page.error.permission.deny", new String[]{"对不起，您无权访问该资源", "en:Sorry, you do not have access to this resource"});
        master.addI18N("page.info.add", new String[]{"添加", "en:Add"});
        master.addI18N("page.info.kv.name", new String[]{"变量名", "en:Name"});
        master.addI18N("page.info.kv.value", new String[]{"值", "en:Value"});
        master.addI18N("page.password.changed", new String[]{"密码修改成功，请重新登录", "en:Password changed successfully, please login again"});
        master.addI18N("page.lang.switch", new String[]{"切换语言", "en:Switch The Language"});

        master.addI18N("page.guide", new String[]{"新手引导", "en:Beginner Guide"});
        master.addI18N("page.guide.previous", new String[]{"上一步", "en:Previous"});
        master.addI18N("page.guide.next", new String[]{"下一步", "en:Next"});
        master.addI18N("page.guide.skip", new String[]{"跳过", "en:Skip"});
        master.addI18N("page.guide.finish", new String[]{"完成", "en:Finish"});

        master.addI18N("page.guide.pwd", new String[]{"首次登录，请修改初始密码", "en:To log in for the first time, please change the initial password"});
        master.addI18N("page.guide.help", new String[]{"查看用户手册，帮助您详细了解如何使用产品", "en:View the user manual to help you learn more about how to use products"});
        master.addI18N("page.guide.home", new String[]{"点击“首页”，可查看产品的名称、版本号、命名空间、运行模式及授权等信息", "en:Click \"Home\" to view the name, version number, namespace, operating mode and authorization of the product"});
        master.addI18N("page.guide.res", new String[]{"展开“资源管理”，可创建应用所需要使用的数据库、会话服务器、应用类库等资源", "en:Expand Resource Management to create resources such as databases, session servers, and application class libraries that your application needs to use"});
        master.addI18N("page.guide.app", new String[]{"展开“应用管理”，可部署、管理应用，并对应用进行升级、备份、回收、迁移等", "en:Expand Application Management to deploy, manage, upgrade, backup, recycle, and migrate applications"});
        master.addI18N("page.guide.monitor", new String[]{"展开“监视管理”，可了解系统的整体健康状况，观测系统的性能瓶颈", "en:Expand Monitoring Management to understand the overall health of the system and observe the performance bottlenecks of the system"});
        master.addI18N("page.guide.diagnosis", new String[]{"展开“诊断管理”，可监控系统指标，在达到设置阈值后进行预警。生成快照、发送告警通知等", "en:Expand Diagnostic Management to monitor system metrics and provide alerts when set thresholds are reached. Generate snapshots, send alarm notifications, and more"});
        master.addI18N("page.guide.log", new String[]{"展开“日志管理”，可对服务器日志、访问日志、审计日志进行配置管理", "en:Expand Log Management to configure and manage server logs, access logs, and audit logs"});
        master.addI18N("page.guide.cluster", new String[]{"点击“集中管理”，可添加添加远程节点，通过负载均衡器、会话服务器、消息服务器等构建集群，以满足企业多种高并发的业务场景", "en:Click \"Centralized Management\" to add remote nodes and build clusters through load balancers, session servers, and message servers to meet multiple high-concurrency business scenarios of enterprises"});
        master.addI18N("page.guide.node", new String[]{"点击“集中管理”, 然后点击菜单创建节点", "en:Click \"Centralized Management\", and then click the menu Create Node"});

        master.addI18N("field.group.product", new String[]{"产品信息", "en:Product Info"});
        master.addI18N("field.group.license", new String[]{"授权信息", "en:License Info"});

        // todo
        master.addI18N("AGREEMENT_HEADER", new String[]{"CharMap.AGREEMENT_HEADER[0]", "en:" + "CharMap.AGREEMENT_HEADER[1]"});
        master.addI18N("AGREEMENT_BODY", new String[]{"CharMap.AGREEMENT_BODY[0]", "en:" + "CharMap.AGREEMENT_BODY[1]"});
    }
}
