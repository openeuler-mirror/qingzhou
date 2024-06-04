<%@ page import="qingzhou.deployer.DeployerConstants" %>
<%@ page pageEncoding="UTF-8" %>

<header class="main-header">
    <nav class="navbar navbar-fixed-top">
        <%-- 顶部 左侧 logo --%>
        <div class="navbar-header">
            <a class="navbar-toggle" href="javascript:void(0);" data-toggle="collapse" data-target=".navbar-collapse"><i
                    class="icon icon-th-large"></i></a>
            <a class="sidebar-toggle" href="javascript:void(0);" data-toggle="push-menu"><i class="icon icon-sliders"></i></a>
            <a class="navbar-brand" href="javascript:void(0);">
                <!-- <img src="<%=contextPath%>/static/images/login/top_logo.png" class="logo" alt=""> -->
                <span class="logo-mini" data-toggle="push-menu" style="display: none;">
                    <i class="icon icon-sliders"></i>
                </span>
            </a>
        </div>

        <%-- 顶部 右侧 按钮 --%>
        <div class="collapse navbar-collapse">
            <div>
                <ul class="nav navbar-nav">
                    <li><span class="console-name"><%=PageBackendService.getMasterAppI18nString("page.index")%></span>
                    </li>
                </ul>
                <ul class="nav navbar-nav navbar-right">
                    <%-- 快捷搜索 --%>
                    <%--<li>
                        <div class="searchBar">
                            <div class="sample">
                                <input type="text" id="searchText" name="search"
                                       placeholder="<%=PageBackendService.getMasterAppI18nString( "page.filter")%>">
                                <a href="javascript:void(0);" class="btn btn-search"><i
                                        class="icon icon-search"></i></a>
                                <div id="searchResult" class="search-list"></div>
                            </div>
                        </div>
                    </li>--%>

                    <%-- 切换语言 --%>
                    <li id="switch-lang" class="dropdown">
                        <a href="javascript:void(0);" class="tooltips" data-toggle="dropdown" data-tip-arrow="bottom"
                           data-tip='<%=PageBackendService.getMasterAppI18nString( "page.lang.switch")%>'>
                            <span class="circle-bg"><i class="icon icon-language"></i></span>
                        </a>
                        <ul class="dropdown-menu dropdown-menu-reset">
                            <%
                                for (Lang lang : Lang.values()) {
                                    out.print("<li>");
                                    out.print(String.format("<a href=\"%s\"><span>%s</span></a>", PageBackendService.encodeURL(response, contextPath + SetI18n.LANG_SWITCH_URI + "/" + lang), lang.info));
                                    out.print("</li>");
                                }
                            %>
                        </ul>
                    </li>
                    <%-- 用户/修改密码 --%>
                    <li>
                        <a id="reset-password-btn"
                           href="<%=PageBackendService.encodeURL( response, (contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath) + RESTController.REST_PREFIX + "/" + ViewManager.htmlView+"/"+ DeployerConstants.MANAGE_TYPE_APP +"/"+ DeployerConstants.MASTER_APP_NAME +"/password/"+ Editable.ACTION_NAME_EDIT)%>"
                           class="tooltips" data-tip='<%=currentUser%>' data-tip-arrow="bottom">
                            <span class="circle-bg">
                                <i class="icon icon-<%=PageBackendService.getModelInfo(DeployerConstants.MASTER_APP_NAME,"user").getIcon()%>"></i>
                            </span>
                        </a>
                    </li>
                    <%-- 注销 --%>
                    <li>
                        <a id="logout-btn"
                           href="<%=PageBackendService.encodeURL( response, contextPath + LoginManager.LOGIN_PATH + "?" + LoginManager.LOGOUT_FLAG)%>"
                           class="tooltips"
                           data-tip='<%=PageBackendService.getMasterAppI18nString( "page.invalidate")%>'
                           data-tip-arrow="bottom">
                            <span class="circle-bg"><i class="icon icon-signout"></i></span>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </nav>
</header>
