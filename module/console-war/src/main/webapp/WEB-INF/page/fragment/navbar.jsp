<%@ page pageEncoding="UTF-8" %>

<header class="main-header">
    <nav class="navbar navbar-fixed-top">
        <%-- 顶部 左侧 logo --%>
        <div class="navbar-header">
            <a class="navbar-toggle" href="javascript:void(0);" data-toggle="collapse" data-target=".navbar-collapse"><i class="icon icon-th-large"></i></a>
            <a class="sidebar-toggle" href="javascript:void(0);" data-toggle="push-menu"><i class="icon icon-sliders"></i></a>
            <a class="navbar-brand" href="javascript:void(0);">
                <img src="<%=contextPath%>/static/images/login/top_logo.png" class="logo" alt="">
                <span class="logo-mini" data-toggle="push-menu" style="display: none;"><i class="icon icon-sliders"></i></span>
            </a>
        </div>

        <%-- 顶部 右侧 按钮 --%>
        <div class="collapse navbar-collapse">
            <div >
                <ul class="nav navbar-nav">
                    <li><span class="console-name"><%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.index")%></span></li>
                </ul>
                <ul class="nav navbar-nav navbar-right">
                   <%-- 快捷搜索 --%>
                    <li>
                       <div class="searchBar">
                            <div class="sample">
                                 <input type="text" id="searchText" name="search" placeholder="<%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.filter")%>">
                                 <a href="javascript:void(0);" class="btn btn-search"><i class="icon icon-search"></i></a>
                                 <div id="searchResult" class="search-list"></div>
                            </div>
                       </div>
                   </li>
                    <%-- 新手引导 --%>
                    <li>
                        <a id="guide-btn" href="javascript:void(0);" class="tooltips" data-tip='<%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.guide")%>' data-tip-arrow="bottom">
                            <span class="circle-bg">
                                <i class="icon icon-lightbulb"></i>
                            </span>
                        </a>
                    </li>

                    <%-- 手册 --%>
                    <li>
                        <a id="help-btn" href='<%=ConsoleUtil.encodeURL(request, response, contextPath + "/" + Constants.MANUAL_PDF)%>' target="_blank"
                        class="tooltips" data-tip='<%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.document")%>' data-tip-arrow="bottom">
                            <span class="circle-bg">
                                <i class="icon icon-file-pdf"></i>
                            </span>
                        </a>
                    </li>

                    <%-- 切换语言 --%>
                    <li id="switch-lang" class="dropdown">
                        <a href="javascript:void(0);" data-toggle="dropdown"
                           class="tooltips" data-tip='<%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.lang.switch")%>' data-tip-arrow="bottom">
                            <span class="circle-bg">
                                <i class="icon icon-language"></i>
                            </span>
                        </a>
                        <ul class="dropdown-menu dropdown-menu-reset">
                            <%
                                for(Lang lang : Lang.values()) {
                                    out.print("<li>");
                                    out.print(String.format("<a href=\"%s\"><span>%s</span></a>", ConsoleUtil.encodeURL(request, response, contextPath + I18nFilter.LANG_SWITCH_URI + "/" + lang), lang.getFullName()));
                                    out.print("</li>");
                                }
                            %>
                        </ul>
                    </li>
                    <%-- 用户/修改密码 --%>
                    <li>
                        <a id="reset-password-btn" href="<%=ConsoleUtil.encodeURL(request, response, ViewManager.htmlView+"/"+ Constants.QINGZHOU_MASTER_APP_NAME +"/"+Constants.MODEL_NAME_password+"/"+ "edit")%>"
                        class="tooltips" data-tip='<%=currentUser%>' data-tip-arrow="bottom">
                            <span class="circle-bg">
                                <i class="icon icon-<%=ConsoleUtil.getModelManager(Constants.QINGZHOU_MASTER_APP_NAME).getModel(Constants.MODEL_NAME_user).icon()%>"></i>
                            </span>
                        </a>
                    </li>
                    <%-- 注销 --%>
                    <li>
                        <a id="logout-btn" href="<%=ConsoleUtil.encodeURL(request, response, contextPath + LoginManager.LOGIN_PATH + "?" + LoginManager.LOGOUT_FLAG)%>"
                        class="tooltips" data-tip='<%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.invalidate")%>' data-tip-arrow="bottom">
                            <span class="circle-bg">
                                <i class="icon icon-signout"></i>
                            </span>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </nav>
</header>
