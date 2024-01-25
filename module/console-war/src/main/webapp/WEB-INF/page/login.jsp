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
        <script type="text/javascript" src="<%=contextPath%>/static/js/jquery.min.js"></script>
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
                <form id="loginForm" method="post" action="<%=PageBackendService.encodeURL( response, contextPath+LoginManager.LOGIN_URI)%>" class="form-group" autocomplete="off">
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
                        <img src="<%=PageBackendService.encodeURL( response, contextPath + VerCode.CAPTCHA_URI)%>" class="captcha" onclick="this.src = '<%=PageBackendService.encodeURL( response, contextPath + VerCode.CAPTCHA_URI)%>' + '?v=' + new Date().getTime()">
                    </div>
                    <%
                    }
                    %>

                    <input type="submit" value='<%=PageBackendService.getMasterAppI18NString( "page.login")%>' class="login_btn">
                    <textarea id="pubkey" rows="3" style="display:none;">
                        <%=PageBackendService.getPublicKeyString()%>
                    </textarea>
                </form>
            </section>
        </main>
        
        <footer class="page-copyright">
            <%=PageBackendService.getMasterAppI18NString( "page.copyright")%>&nbsp;<a href="https://www.openeuler.org/" target="_blank">www.openeuler.org</a>
            &nbsp;<a href="javascript:about();">关于</a>
        </footer>

        <textarea id="markedText" style="display:none;">
轻舟软件运行时管理平台
========================

轻舟，提供了一套开发Web管理软件的基础平台，基于声明式API、充血模型等思想，开发者只需编写后端业务模型代码，即可自动获得前端页面，并自动化实现参数校验、REST接口、远程管理、角色权限、页面国际化、云上对接等能力。

软件架构
-------------------

![软件架构](static/images/architecture.jpg)

免责声明
-------------

本项目基于[MulanPSL-2.0](http://license.coscl.org.cn/MulanPSL2)开源许可协议，代码免费且已开源。使用时请遵循相关开源许可协议!

+ 不得将 qingzhou 用于危害国家安全、荣誉和利益的行为，不能以任何形式用于非法为目的的行为,否则后果自负
+ 虽然本项目在开发过程中很注重安全，但是您仍然需要了解：是软件皆有漏洞，任何人都无法保证软件100%没有漏洞。所以由本软件漏洞造成损失不予赔偿，同时也不承担任何因使用本软件而产生的相关法律责任。也请在软件上线前进行必要的安全监测，避免安全问题发生。

鸣谢
-------------

本项目开发过程中，借鉴和引用了许多优秀项目的设计思路或代码库文件等，在此特别感谢原作者的贡献付出！同时也感谢众多小伙伴提出项目问题及贡献的代码.

主要引用到的项目：
+ tomcat (https://tomcat.apache.org/)

+ jquery (https://jquery.com/)

+ openzui (https://openzui.com/)

+ layer (https://gitee.com/layui/layer.git)

+ multiple-select (http://multiple-select.wenzhixin.net.cn)

+ marked (https://github.com/markedjs/marked/)
        
        </textarea>

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
            });
            function escapeFrame() {
                if (window.top.location.href !== window.location.href) {
                    window.top.location.reload();
                }
            };
            function about() {
                layer.open({type: 1, shade: 0.2, shadeClose: true, maxmin: true, title: ["关于", 'font-size:20px;text-align:left;font-weight:bold;'], area: ['65%', '80%'], 
                content: "<div style=\"padding: 30px;\"><div id=\"markedShow\"></div></div>"});
                document.getElementById("markedShow").innerHTML = marked.parse(document.getElementById("markedText").value);
                $("#markedShow a").attr("target", "_blank");
            };
        </script>
    </body>

</html>