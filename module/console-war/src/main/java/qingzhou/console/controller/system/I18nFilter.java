package qingzhou.console.controller.system;

import qingzhou.console.ConsoleI18n;
import qingzhou.console.ConsoleUtil;
import qingzhou.console.I18n;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.view.ViewManager;
import qingzhou.framework.api.Lang;
import qingzhou.framework.pattern.Filter;
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
        ConsoleI18n.addI18N(LOGIN_ERROR_MSG_KEY, new String[]{"登录失败，用户名或密码错误。当前登录失败 %s 次，连续失败 %s 次，账户将锁定", "en:Login failed, wrong username or password. The current login failed %s times, and the account will be locked after %s consecutive failures"});
        ConsoleI18n.addI18N(LOCKED_MSG_KEY, new String[]{"连续登录失败 %s 次，账户已经锁定，请 %s 分钟后重试", "en:Login failed %s times in a row, account is locked, please try again in %s minutes"});
        ConsoleI18n.addI18N(TWO_FA_MSG_KEY, new String[]{"双因子认证认证失败：动态密码错误", "en:Two-factor authentication authentication failed: dynamic password error"});
        ConsoleI18n.addI18N(ACCEPT_AGREEMENT_MSG_KEY_MISSING, new String[]{"请输入同意本产品《许可协议》的参数：" + LOGIN_ACCEPT_AGREEMENT + "=true", "en:Please enter the parameters of agreeing to the License Agreement of this product:" + LOGIN_ACCEPT_AGREEMENT + "=true"});
        ConsoleI18n.addI18N(ACCEPT_AGREEMENT_MSG_KEY, new String[]{"请确保您已阅读并同意本产品的《许可协议》", "en:Please ensure that you have read and agree to the <License Agreement> for this product"});
        ConsoleI18n.addI18N("jmx.credentials.element.isNull", new String[]{"用户名或密码不能为空", "en:The user name or password cannot be empty"});

        ConsoleI18n.addI18N("page.index", new String[]{"管理控制台", "en:Console"});
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
        ConsoleI18n.addI18N("page.copyright", new String[]{"版权所有 © 2023 openEuler 保留一切权利", "en:Copyright © 2023 openEuler. All rights reserved."});
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
        ConsoleI18n.addI18N("page.error.permission.deny", new String[]{"对不起，您无权访问该资源", "en:Sorry, you do not have access to this resource"});
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

        ConsoleI18n.addI18N("AGREEMENT_HEADER", new String[]{"木兰宽松许可证, 第2版", "en:Mulan Permissive Software License, Version 2"});
        ConsoleI18n.addI18N("AGREEMENT_BODY", new String[]{
                "\n" +
                        "   木兰宽松许可证， 第2版 \n" +
                        "   2020年1月 http://license.coscl.org.cn/MulanPSL2\n" +
                        "\n" +
                        "\n" +
                        "   您对“软件”的复制、使用、修改及分发受木兰宽松许可证，第2版（“本许可证”）的如下条款的约束：\n" +
                        "\n" +
                        "   0. 定义\n" +
                        "\n" +
                        "      “软件”是指由“贡献”构成的许可在“本许可证”下的程序和相关文档的集合。\n" +
                        "\n" +
                        "      “贡献”是指由任一“贡献者”许可在“本许可证”下的受版权法保护的作品。\n" +
                        "\n" +
                        "      “贡献者”是指将受版权法保护的作品许可在“本许可证”下的自然人或“法人实体”。\n" +
                        "\n" +
                        "      “法人实体”是指提交贡献的机构及其“关联实体”。\n" +
                        "\n" +
                        "      “关联实体”是指，对“本许可证”下的行为方而言，控制、受控制或与其共同受控制的机构，此处的控制是指有受控方或共同受控方至少50%直接或间接的投票权、资金或其他有价证券。\n" +
                        "\n" +
                        "   1. 授予版权许可\n" +
                        "\n" +
                        "      每个“贡献者”根据“本许可证”授予您永久性的、全球性的、免费的、非独占的、不可撤销的版权许可，您可以复制、使用、修改、分发其“贡献”，不论修改与否。\n" +
                        "\n" +
                        "   2. 授予专利许可\n" +
                        "\n" +
                        "      每个“贡献者”根据“本许可证”授予您永久性的、全球性的、免费的、非独占的、不可撤销的（根据本条规定撤销除外）专利许可，供您制造、委托制造、使用、许诺销售、销售、进口其“贡献”或以其他方式转移其“贡献”。前述专利许可仅限于“贡献者”现在或将来拥有或控制的其“贡献”本身或其“贡献”与许可“贡献”时的“软件”结合而将必然会侵犯的专利权利要求，不包括对“贡献”的修改或包含“贡献”的其他结合。如果您或您的“关联实体”直接或间接地，就“软件”或其中的“贡献”对任何人发起专利侵权诉讼（包括反诉或交叉诉讼）或其他专利维权行动，指控其侵犯专利权，则“本许可证”授予您对“软件”的专利许可自您提起诉讼或发起维权行动之日终止。\n" +
                        "\n" +
                        "   3. 无商标许可\n" +
                        "\n" +
                        "      “本许可证”不提供对“贡献者”的商品名称、商标、服务标志或产品名称的商标许可，但您为满足第4条规定的声明义务而必须使用除外。\n" +
                        "\n" +
                        "   4. 分发限制\n" +
                        "\n" +
                        "      您可以在任何媒介中将“软件”以源程序形式或可执行形式重新分发，不论修改与否，但您必须向接收者提供“本许可证”的副本，并保留“软件”中的版权、商标、专利及免责声明。\n" +
                        "\n" +
                        "   5. 免责声明与责任限制\n" +
                        "\n" +
                        "      “软件”及其中的“贡献”在提供时不带任何明示或默示的担保。在任何情况下，“贡献者”或版权所有者不对任何人因使用“软件”或其中的“贡献”而引发的任何直接或间接损失承担责任，不论因何种原因导致或者基于何种法律理论，即使其曾被建议有此种损失的可能性。 \n" +
                        "\n" +
                        "   6. 语言\n" +
                        "      “本许可证”以中英文双语表述，中英文版本具有同等法律效力。如果中英文版本存在任何冲突不一致，以中文版为准。\n" +
                        "\n" +
                        "   条款结束 \n" +
                        "\n" +
                        "   如何将木兰宽松许可证，第2版，应用到您的软件\n" +
                        "   \n" +
                        "   如果您希望将木兰宽松许可证，第2版，应用到您的新软件，为了方便接收者查阅，建议您完成如下三步：\n" +
                        "\n" +
                        "      1， 请您补充如下声明中的空白，包括软件名、软件的首次发表年份以及您作为版权人的名字；\n" +
                        "\n" +
                        "      2， 请您在软件包的一级目录下创建以“LICENSE”为名的文件，将整个许可证文本放入该文件中；\n" +
                        "\n" +
                        "      3， 请将如下声明文本放入每个源文件的头部注释中。\n" +
                        "\n" +
                        "   Copyright (c) [Year] [name of copyright holder]\n" +
                        "   [Software Name] is licensed under Mulan PSL v2.\n" +
                        "   You can use this software according to the terms and conditions of the Mulan PSL v2. \n" +
                        "   You may obtain a copy of Mulan PSL v2 at:\n" +
                        "            http://license.coscl.org.cn/MulanPSL2 \n" +
                        "   THIS SOFTWARE IS PROVIDED ON AN \"AS IS\" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.  \n" +
                        "   See the Mulan PSL v2 for more details.  \n",

                "en:" + "\n" +
                        "   Mulan Permissive Software License, Version 2 (Mulan PSL v2)\n" +
                        "   January 2020 http://license.coscl.org.cn/MulanPSL2\n" +
                        "\n" +
                        "   Your reproduction, use, modification and distribution of the Software shall be subject to Mulan PSL v2 (this License) with the following terms and conditions: \n" +
                        "   \n" +
                        "   0. Definition\n" +
                        "   \n" +
                        "      Software means the program and related documents which are licensed under this License and comprise all Contribution(s). \n" +
                        "   \n" +
                        "      Contribution means the copyrightable work licensed by a particular Contributor under this License.\n" +
                        "   \n" +
                        "      Contributor means the Individual or Legal Entity who licenses its copyrightable work under this License.\n" +
                        "   \n" +
                        "      Legal Entity means the entity making a Contribution and all its Affiliates.\n" +
                        "   \n" +
                        "      Affiliates means entities that control, are controlled by, or are under common control with the acting entity under this License, 'control' means direct or indirect ownership of at least fifty percent (50%) of the voting power, capital or other securities of controlled or commonly controlled entity.\n" +
                        "\n" +
                        "   1. Grant of Copyright License\n" +
                        "\n" +
                        "      Subject to the terms and conditions of this License, each Contributor hereby grants to you a perpetual, worldwide, royalty-free, non-exclusive, irrevocable copyright license to reproduce, use, modify, or distribute its Contribution, with modification or not.\n" +
                        "\n" +
                        "   2. Grant of Patent License \n" +
                        "\n" +
                        "      Subject to the terms and conditions of this License, each Contributor hereby grants to you a perpetual, worldwide, royalty-free, non-exclusive, irrevocable (except for revocation under this Section) patent license to make, have made, use, offer for sale, sell, import or otherwise transfer its Contribution, where such patent license is only limited to the patent claims owned or controlled by such Contributor now or in future which will be necessarily infringed by its Contribution alone, or by combination of the Contribution with the Software to which the Contribution was contributed. The patent license shall not apply to any modification of the Contribution, and any other combination which includes the Contribution. If you or your Affiliates directly or indirectly institute patent litigation (including a cross claim or counterclaim in a litigation) or other patent enforcement activities against any individual or entity by alleging that the Software or any Contribution in it infringes patents, then any patent license granted to you under this License for the Software shall terminate as of the date such litigation or activity is filed or taken.\n" +
                        "\n" +
                        "   3. No Trademark License\n" +
                        "\n" +
                        "      No trademark license is granted to use the trade names, trademarks, service marks, or product names of Contributor, except as required to fulfill notice requirements in Section 4.\n" +
                        "\n" +
                        "   4. Distribution Restriction\n" +
                        "\n" +
                        "      You may distribute the Software in any medium with or without modification, whether in source or executable forms, provided that you provide recipients with a copy of this License and retain copyright, patent, trademark and disclaimer statements in the Software.\n" +
                        "\n" +
                        "   5. Disclaimer of Warranty and Limitation of Liability\n" +
                        "\n" +
                        "      THE SOFTWARE AND CONTRIBUTION IN IT ARE PROVIDED WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED. IN NO EVENT SHALL ANY CONTRIBUTOR OR COPYRIGHT HOLDER BE LIABLE TO YOU FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO ANY DIRECT, OR INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING FROM YOUR USE OR INABILITY TO USE THE SOFTWARE OR THE CONTRIBUTION IN IT, NO MATTER HOW IT’S CAUSED OR BASED ON WHICH LEGAL THEORY, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.\n" +
                        "\n" +
                        "   6. Language\n" +
                        "\n" +
                        "      THIS LICENSE IS WRITTEN IN BOTH CHINESE AND ENGLISH, AND THE CHINESE VERSION AND ENGLISH VERSION SHALL HAVE THE SAME LEGAL EFFECT. IN THE CASE OF DIVERGENCE BETWEEN THE CHINESE AND ENGLISH VERSIONS, THE CHINESE VERSION SHALL PREVAIL.\n" +
                        "\n" +
                        "   END OF THE TERMS AND CONDITIONS\n" +
                        "\n" +
                        "   How to Apply the Mulan Permissive Software License, Version 2 (Mulan PSL v2) to Your Software\n" +
                        "\n" +
                        "      To apply the Mulan PSL v2 to your work, for easy identification by recipients, you are suggested to complete following three steps:\n" +
                        "\n" +
                        "      i Fill in the blanks in following statement, including insert your software name, the year of the first publication of your software, and your name identified as the copyright owner; \n" +
                        "\n" +
                        "      ii Create a file named \"LICENSE\" which contains the whole context of this License in the first directory of your software package;\n" +
                        "\n" +
                        "      iii Attach the statement to the appropriate annotated syntax at the beginning of each source file.\n" +
                        "\n" +
                        "\n" +
                        "   Copyright (c) [Year] [name of copyright holder]\n" +
                        "   [Software Name] is licensed under Mulan PSL v2.\n" +
                        "   You can use this software according to the terms and conditions of the Mulan PSL v2. \n" +
                        "   You may obtain a copy of Mulan PSL v2 at:\n" +
                        "               http://license.coscl.org.cn/MulanPSL2 \n" +
                        "   THIS SOFTWARE IS PROVIDED ON AN \"AS IS\" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.  \n" +
                        "   See the Mulan PSL v2 for more details.  \n"
        });
    }
}
