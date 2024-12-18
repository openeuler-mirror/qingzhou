<%@ page pageEncoding="UTF-8" %>

<header class="main-header">
    <nav class="navbar navbar-fixed-top">
        <%-- 顶部 左侧 logo --%>
        <div class="navbar-header">
            <a class="navbar-toggle" href="javascript:void(0);" data-toggle="collapse" data-target=".navbar-collapse"><i
                    class="icon icon-th-large"></i></a>
            <a class="sidebar-toggle" href="javascript:void(0);" data-toggle="push-menu"><i
                    class="icon icon-sliders"></i></a>
            <a class="navbar-brand" href="javascript:void(0);">
                <img src="<%=contextPath%>/static/images/login/top_logo.svg" class="logo" alt="">
                <span class="logo-mini" data-toggle="push-menu" style="display: none;">
                    <i class="icon icon-sliders"></i>
                </span>
            </a>
        </div>

        <%-- 顶部 右侧 按钮 --%>
        <div class="collapse navbar-collapse">
            <div>
                <ul class="nav navbar-nav">
                    <li><span class="console-name"><%=I18n.getKeyI18n("page.index")%></span>
                    </li>
                </ul>
                <ul class="nav navbar-nav navbar-right">
                    <%-- 明暗主题切换 --%>
                    <li id="switch-mode" class="switch-btn">
                        <a id="switch-mode-btn"
                           href="javascript:void(0);"
                           theme="<%= themeMode == null ? "" : themeMode %>"
                           themeUrl="<%=RESTController.encodeURL(response, contextPath + Theme.URI_THEME + "/" + ((themeMode == null || themeMode.isEmpty()) ? "dark" : ""))%>"
                           class="tooltips" data-tip="<%=I18n.getKeyI18n("page.thememode")%>" data-tip-arrow="bottom">
                        <span class="circle-bg">
                            <i class="icon <%=(themeMode == null || themeMode.isEmpty()) ? "icon-moon" : "icon-sun"%>"></i>
                        </span>
                        </a>
                    </li>
                    <%-- 切换语言 --%>
                    <li id="switch-lang" class="dropdown">
                        <a href="javascript:void(0);" class="tooltips" data-toggle="dropdown" data-tip-arrow="bottom"
                           data-tip='<%=I18n.getKeyI18n( "page.lang.switch")%>'>
                            <span class="circle-bg"><i class="icon icon-language"></i></span>
                        </a>
                        <ul class="dropdown-menu dropdown-menu-reset">
                            <%
                                for (Lang lang : Lang.values()) {
                                    out.print("<li>");
                                    out.print(String.format("<a href=\"%s\"><span>%s</span></a>", RESTController.encodeURL(response, contextPath + I18n.LANG_SWITCH_URI + "/" + lang), lang.info));
                                    out.print("</li>");
                                }
                            %>
                        </ul>
                    </li>
                    <%-- 用户/修改密码 --%>
                    <li>
                        <a id="reset-password-btn"
                           href="<%=RESTController.encodeURL( response, (contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath) + DeployerConstants.REST_PREFIX + "/" + HtmlView.FLAG + "/" + DeployerConstants.APP_SYSTEM +"/" + DeployerConstants.MODEL_PASSWORD + "/" + Update.ACTION_EDIT)%>"
                           class="tooltips" data-tip='<%=LoginManager.getLoggedUser(session).getName()%>' data-tip-arrow="bottom">
                            <span class="circle-bg">
                                <i class="icon icon-<%=SystemController.getModelInfo(DeployerConstants.APP_SYSTEM, DeployerConstants.MODEL_USER).getIcon()%>"></i>
                            </span>
                        </a>
                    </li>
                    <%-- 注销 --%>
                    <li>
                        <a id="logout-btn"
                           href="<%=RESTController.encodeURL( response, contextPath + LoginManager.LOGIN_PATH + "?" + LoginManager.LOGOUT_FLAG)%>"
                           class="tooltips"
                           data-tip='<%=I18n.getKeyI18n( "page.invalidate")%>'
                           data-tip-arrow="bottom">
                            <span class="circle-bg"><i class="icon icon-signout"></i></span>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </nav>
</header>
