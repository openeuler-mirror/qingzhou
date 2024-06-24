<%@ page import="qingzhou.console.i18n.I18n" %>
<%@ page import="qingzhou.console.page.PageBackendService" %>
<%@ page import="qingzhou.console.login.LoginManager" %>
<%@ page import="qingzhou.console.ConsoleConstants" %>
<%@ page import="qingzhou.console.login.vercode.VerCode" %>
<%@ page import="qingzhou.console.controller.About" %>
<%@ page import="qingzhou.console.controller.rest.AsymmetricDecryptor" %>
<%@ page import="qingzhou.console.controller.rest.RESTController" %>
<%@ page import="qingzhou.deployer.DeployerConstants" %>

<%@ page pageEncoding="UTF-8" %>
<%
    String contextPath = request.getContextPath();
    session.invalidate();
    I18n.setI18nLang(request, I18n.DEFAULT_LANG);
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <base href="<%=contextPath%>/">
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="https://gitee.com/openeuler/qingzhou">
    <title>Qingzhou Console</title>
    <link type="image/x-icon" rel="shortcut icon" href="<%=contextPath%>/static/images/favicon.svg">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/lib/zui/css/zui.min.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/css/login.css">
    <script type="text/javascript" src="<%=contextPath%>/static/js/jquery.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/jquery.form.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/zui/js/zui.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/jsencrypt.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/msg.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/layer/layer.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/main.js"></script>
</head>

<body class="login_body" onload="escapeFrame();">
<header class="login-header">
    <img src="<%=contextPath%>/static/images/login/top_logo.svg" class="login-top-logo" alt="">
    <div class="login-right-lang"></div>
</header>
<main class="page page-login text-center">
    <section class="body_left">
        <div class="body_logo"></div>
        <div class="body_img"></div>
    </section>
    <section class="panel-body">
        <div class="logo"><%=PageBackendService.getMasterAppI18nString("page.userlogin")%>
        </div>
        <form id="loginForm" method="post"
              action="<%=PageBackendService.encodeURL( response, contextPath+LoginManager.LOGIN_URI)%>"
              class="form-group" autocomplete="off">
            <div class="input-control has-icon-left">
                <input value="qingzhou" type="text" id="<%=LoginManager.LOGIN_USER%>" name="<%=LoginManager.LOGIN_USER%>" required
                       class="form-control" placeholder="<%=PageBackendService.getMasterAppI18nString( "model.field.user.name")%>" autofocus>
                <label for="<%=LoginManager.LOGIN_USER%>" class="input-control-icon-left" style="line-height: 44px;">
                    <i class="icon icon-<%=PageBackendService.getAppInfo(DeployerConstants.MASTER_APP_NAME).getModelInfo("user").getIcon()%> "></i>
                </label>
            </div>
            <div class="input-control has-icon-left">
                <input value="qingzhou123.com" type="text" id="<%=LoginManager.LOGIN_PASSWORD%>_txt" data-type="password" class="form-control"
                       placeholder="<%=PageBackendService.getMasterAppI18nString( "model.field.user.password")%>" required
                       onchange="document.getElementById('<%=LoginManager.LOGIN_PASSWORD%>').value = this.value;" dotted>
                <input value="qingzhou123.com" type="hidden" id="<%=LoginManager.LOGIN_PASSWORD%>" name="<%=LoginManager.LOGIN_PASSWORD%>">
                <label for="<%=LoginManager.LOGIN_PASSWORD%>_txt" class="input-control-icon-left" style="line-height: 44px;">
                    <i class="icon icon-<%=PageBackendService.getAppInfo(DeployerConstants.MASTER_APP_NAME).getModelInfo("password").getIcon()%>"></i>
                </label>
                <label id="<%=LoginManager.LOGIN_PASSWORD%>_eye" for="<%=LoginManager.LOGIN_PASSWORD%>_txt"
                       class="input-control-icon-right" style="margin-right: 28px; margin-top: 5px; cursor: pointer;">
                    <i class="icon icon-eye-close"></i>
                </label>
            </div>
            <div class="input-control has-icon-left">
                <input type="text" id="<%=ConsoleConstants.LOGIN_2FA%>_txt" class="form-control"
                       placeholder="<%=PageBackendService.getMasterAppI18nString( "page.info.2fa")%>"
                       onchange="document.getElementById('<%=ConsoleConstants.LOGIN_2FA%>').value = this.value;">
                <input type="hidden" id="<%=ConsoleConstants.LOGIN_2FA%>" name="<%=ConsoleConstants.LOGIN_2FA%>">
                <label for="<%=ConsoleConstants.LOGIN_2FA%>_txt" class="input-control-icon-left"
                       style="line-height: 44px;"><i class="icon icon-shield"></i></label>
            </div>

            <%
                if (request.getParameter(VerCode.SHOW_CAPTCHA_FLAG) != null) {
            %>
            <div class="input-control has-icon-left">
                <input type="text" id="<%=VerCode.CAPTCHA%>" name="<%=VerCode.CAPTCHA%>" class="form-control"
                       style="width:250px;display:inline-block;float:left;"
                       placeholder="<%=PageBackendService.getMasterAppI18nString( "page.vercode")%>">
                <label class="input-control-icon-left" style="line-height: 44px;"><i class="icon icon-shield"></i></label>
                <img src="<%=PageBackendService.encodeURL( response, contextPath + VerCode.CAPTCHA_URI)%>" class="captcha"
                     onclick="this.src = '<%=PageBackendService.encodeURL( response, contextPath + VerCode.CAPTCHA_URI)%>' + '?v=' + new Date().getTime()">
            </div>
            <%
                }
            %>

            <input type="submit" value='<%=PageBackendService.getMasterAppI18nString( "page.login")%>' class="login_btn">
            <textarea id="pubkey" rows="3" style="display:none;">
                <%=AsymmetricDecryptor.getPublicKeyString()%>
            </textarea>
        </form>
    </section>
</main>

<footer class="page-copyright">
    <%=PageBackendService.getMasterAppI18nString("page.copyright")%>&nbsp;<a href="https://gitee.com/openeuler/qingzhou"
                                                                             target="_blank">gitee.com/openeuler/qingzhou</a>
    &nbsp;<a href="javascript:about();">关于</a>
</footer>

<script src="<%=contextPath%>/static/lib/marked/marked.min.js"></script>
<script type="text/javascript">
    var reg_pwd = /[^a-zA-Z0-9!@#$%^&*()_+-={}\[\]|;:\'",./<>?"]/g;
    $("input[data-type='password']").bind("input", function () {
        $(this).val($(this).val().replace(reg_pwd, ""));
    }).bind("change", function () {
        $(this).val($(this).val().replace(reg_pwd, ""));
    });
    $("#<%=LoginManager.LOGIN_PASSWORD%>_eye").click(function () {
        if ($("i", this).hasClass("icon-eye-open")) {
            $("i", this).removeClass("icon-eye-open").addClass("icon-eye-close");
            $("#<%=LoginManager.LOGIN_PASSWORD%>_txt").attr("dotted", "");
        } else {
            $("i", this).removeClass("icon-eye-close").addClass("icon-eye-open");
            $("#<%=LoginManager.LOGIN_PASSWORD%>_txt").removeAttr("dotted");
        }
    });
    $("#loginForm").submit(function (e) {
        var encrypt = new JSEncrypt({"default_key_size":<%=AsymmetricDecryptor.getKeySize()%>});
        encrypt.setPublicKey($('#pubkey').val());
        var inputs = $("#loginForm").find("input");
        for (var i = 0; i < inputs.length; i++) {
            var input = inputs[i];
            $(input).change();
            if (input.id === "<%=LoginManager.LOGIN_PASSWORD%>" || input.id === "<%=ConsoleConstants.LOGIN_2FA%>") {
                $(input).val(encrypt.encryptLong2($(input).val()));
            }
        }
        return true;
    });

    function escapeFrame() {
        if (window.top.location.href !== window.location.href) {
            window.top.location.reload();
        }
    }

    function about() {
        $.post("<%=contextPath + About.ABOUT_URI%>", {}, function (text) {
            layer.open({
                type: 1,
                shade: 0.2,
                shadeClose: true,
                maxmin: true,
                title: ["关于", 'font-size:20px;text-align:left;font-weight:bold;'],
                area: ['65%', '80%'],
                content: "<div style=\"padding: 30px;\"><div id=\"markedShow\"></div></div>"
            });
            document.getElementById("markedShow").innerHTML = marked.parse(text);
            $("#markedShow a").attr("target", "_blank");
        }, "text");
    }
</script>
</body>
<%--公用“重定向页面”消息提示--%>
<%
    String common_msg = request.getParameter(RESTController.MSG_FLAG);
    common_msg = LoginManager.retrieveI18nMsg(common_msg);
    if (common_msg != null) {
%>
<script>
    $(document).ready(function () {
        window.setTimeout(function () {
            var common_msgIndex = showError("<%=common_msg%>");
                // 记录最后一次通知弹窗
                try {
                    $(getActiveTabContent()).attr("showInfoIndex", common_msgIndex);
                } catch (e) {
                    // login.jsp
                }
            }, 350);
        });
</script>
<%
    }
%>
</html>
