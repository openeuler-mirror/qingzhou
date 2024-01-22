<%@ page pageEncoding="UTF-8" %>
<%
String contextPath = request.getContextPath();
session.invalidate();
I18nFilter.setI18nLang(request, I18n.DEFAULT_LANG);
%>
<!DOCTYPE html>
<html lang="zh-CN">
    <head>
        <base href="<%=contextPath%>/">
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="author" content="www.openeuler.org">
        <title>QingZhou Console</title>
        <link type="image/x-icon" rel="shortcut icon" href="<%=contextPath%>/static/images/favicon.svg">
        <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/lib/zui/css/zui.min.css">
        <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/css/login.css">
        <%--注意：后面的<!--\>一定不能省略，否则在 IE 之外的浏览器就无法加载 jQuery --%>
        <!--[if gte IE 9]><!-->
        <script type="text/javascript" src="<%=contextPath%>/static/js/jquery.min.js"></script>
        <!--<![endif]-->
        <!--[if lt IE 9]>
            <script type="text/javascript" src="<%=contextPath%>/static/js/jquery.suport.js"></script>
            <script type="text/javascript" src="<%=contextPath%>/static/lib/zui/lib/ieonly/html5shiv.js"></script>
            <script type="text/javascript" src="<%=contextPath%>/static/lib/zui/lib/ieonly/respond.js"></script>
        <![endif]-->
        <script type="text/javascript" src="<%=contextPath%>/static/js/jquery.form.min.js"></script>
        <script type="text/javascript" src="<%=contextPath%>/static/lib/zui/js/zui.min.js"></script>
        <script type="text/javascript" src="<%=contextPath%>/static/js/jsencrypt.min.js"></script>
        <script type="text/javascript" src="<%=contextPath%>/static/js/msg.js"></script>
        <script type="text/javascript" src="<%=contextPath%>/static/lib/layer/layer.js"></script>
        <script type="text/javascript" src="<%=contextPath%>/static/js/main.js"></script>
    </head>

    <%@ include file="fragment/head.jsp" %>

    <body class="login_body" onload="escapeFrame();">
        <header class="login-header">
            <img src="<%=contextPath%>/static/images/login/top_logo.png" class="login-top-logo" alt="">
            <div class="login-right-lang"></div>
        </header>
        <main class="page page-login text-center">
            <section class="body_left">
                <div class="body_logo"></div>
                <div class="body_img"></div>
            </section>
            <section class="panel-body">
                <div class="logo"><%=PageBackendService.getMasterAppI18NString( "page.userlogin")%></div>
                <form id="loginForm" method="post" action="<%=PageBackendService.encodeURL(request, response, contextPath+LoginManager.LOGIN_URI)%>" class="form-group" autocomplete="off">
                    <div class="input-control has-icon-left">
                        <input value="thanos" type="text" id="<%=LoginManager.LOGIN_USER%>" name="<%=LoginManager.LOGIN_USER%>" class="form-control" placeholder="name<%=PageBackendService.getMasterAppI18NString( "model.field.user.name")%>" autofocus required>
                        <label for="<%=LoginManager.LOGIN_USER%>" class="input-control-icon-left" style="line-height: 44px;"><i class="icon icon-<%=modelManager.getModel("user").icon()%> "></i></label>
                    </div>
                    <div class="input-control has-icon-left">
                        <input value="thanos123.com" type="text" id="<%=LoginManager.LOGIN_PASSWORD%>_txt" data-type="password" class="form-control" placeholder="password<%=PageBackendService.getMasterAppI18NString( "model.field.user.password")%>" dotted onchange="document.getElementById('<%=LoginManager.LOGIN_PASSWORD%>').value = this.value;">
                        <input value="thanos123.com" type="hidden" id="<%=LoginManager.LOGIN_PASSWORD%>" name="<%=LoginManager.LOGIN_PASSWORD%>">
                        <label for="<%=LoginManager.LOGIN_PASSWORD%>_txt" class="input-control-icon-left" style="line-height: 44px;"><i class="icon icon-<%=modelManager.getModel("password").icon()%>"></i></label>
                        <label id="<%=LoginManager.LOGIN_PASSWORD%>_eye" for="<%=LoginManager.LOGIN_PASSWORD%>_txt" class="input-control-icon-right" style="margin-right: 28px; margin-top: 5px; cursor: pointer;"><i class="icon icon-eye-close"></i></label>
                    </div>
                    <div class="input-control has-icon-left">
                        <input type="text" id="<%=ConsoleConstants.LOGIN_2FA%>_txt" class="form-control" placeholder="<%=PageBackendService.getMasterAppI18NString( "page.info.2fa")%>" onchange="document.getElementById('<%=ConsoleConstants.LOGIN_2FA%>').value = this.value;">
                        <input type="hidden" id="<%=ConsoleConstants.LOGIN_2FA%>" name="<%=ConsoleConstants.LOGIN_2FA%>">
                        <label for="<%=ConsoleConstants.LOGIN_2FA%>_txt" class="input-control-icon-left" style="line-height: 44px;"><i class="icon icon-shield"></i></label>
                    </div>
                    
                    <%
                    if (request.getParameter(VerCode.SHOW_CAPTCHA_FLAG) != null) {
                    %>
                    <div class="input-control has-icon-left">
                        <input type="text" id="<%=VerCode.CAPTCHA%>" name="<%=VerCode.CAPTCHA%>" class="form-control" required style="width:250px;display:inline-block;float:left;" placeholder="<%=PageBackendService.getMasterAppI18NString( "page.vercode")%>">
                        <label for="randomcode" class="input-control-icon-left" style="line-height: 44px;"><i class="icon icon-shield"></i></label>
                        <img src="<%=PageBackendService.encodeURL(request, response, contextPath + VerCode.CAPTCHA_URI)%>" class="captcha" onclick="this.src = '<%=PageBackendService.encodeURL(request, response, contextPath + VerCode.CAPTCHA_URI)%>' + '?v=' + new Date().getTime()">
                    </div>
                    <%
                    }
                    %>

                    <input type="submit" value='<%=PageBackendService.getMasterAppI18NString( "page.login")%>' class="login_btn">
                    <div style="display:block;">
                        <div style="text-align:right; padding-right:30px; padding-top:12px;">
                            <input checked type="checkbox" id="<%=LoginManager.LOGIN_ACCEPT_AGREEMENT%>" name="<%=LoginManager.LOGIN_ACCEPT_AGREEMENT%>" value="false" style="cursor:pointer;vertical-align:middle;">
                            <label for="<%=LoginManager.LOGIN_ACCEPT_AGREEMENT%>" style="cursor:pointer; vertical-align:middle; margin-top: 10px;">已阅读并同意</label>
                            <label style="vertical-align:middle; margin-top: 10px;">《<a href="javascript:void(0);" onclick="agreementInfo();">许可协议</a>》</label>
                        </div>
                        <div id="agreementTxt" style="display:none;">
                            <pre align="left">

                                <%
                                for (Lang lang : Lang.values()) {
                                    %>
                                    <h3 align="center"><%=PageBackendService.getMasterAppI18NString( "AGREEMENT_HEADER", lang)%></h3>
                                    <%=PageBackendService.getMasterAppI18NString( "AGREEMENT_BODY", lang)%>
                                    <br><br>
                                    <p align="center">----------------------------------------------------------------------------</p>
                                    <%
                                }
                                %>

                            </pre>
                        </div>
                    </div>
                    <textarea id="pubkey" rows="3" style="display:none;">
                        <%=PageBackendService.getPublicKeyString()%>
                    </textarea>
                </form>
            </section>
        </main>
        
        <footer class="page-copyright">
            <%=PageBackendService.getMasterAppI18NString( "page.copyright")%>&nbsp;<a href="https://www.openeuler.org/" target="_blank">www.openeuler.org</a>
        </footer>
        
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
                if ($("#<%=LoginManager.LOGIN_ACCEPT_AGREEMENT%>").is(":checked")) {
                    $("#<%=LoginManager.LOGIN_ACCEPT_AGREEMENT%>").val("true");
                    var encrypt = new JSEncrypt({"default_key_size":<%=PageBackendService.getKeySize()%>});
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
                } else {
                    $("#<%=LoginManager.LOGIN_ACCEPT_AGREEMENT%>").val("false");
                    layer.msg("<%=PageBackendService.getMasterAppI18NString( LoginManager.ACCEPT_AGREEMENT_MSG_KEY)%>", function () {});
                    return false;
                }
            });
            function escapeFrame() {
                if (window.top.location.href !== window.location.href) {
                    window.top.location.reload();
                }
            };
            function agreementInfo() {
                layer.open({type: 1, shade: 0.2, shadeClose: true, title: ["许可协议", 'font-size:14px;text-align:left;font-weight:bold;'], area: ['35%', '70%'], content: $("#agreementTxt")});
            };
        </script>
    </body>

</html>