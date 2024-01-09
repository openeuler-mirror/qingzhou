<%@ page pageEncoding="UTF-8" %>

<%-- 左侧菜单 --%>
<aside class="main-sidebar">
    <div class="sidebar sidebar-scroll">
        <ul class="sidebar-menu" data-widget="tree">
            <%
            String curAppName = ConsoleConstants.MASTER_APP_NAME.equals(qzRequest.getTargetName()) ? ConsoleConstants.MASTER_APP_NAME : qzRequest.getAppName();
            List<Properties> appMenuList = ConsoleUtil.getAppMenuList(currentUser, curAppName);
            String curTargetName = ConsoleConstants.MASTER_APP_NAME.equals(qzRequest.getTargetName()) ? ConsoleConstants.LOCAL_NODE_NAME : qzRequest.getTargetName();
            out.print(ConsoleUtil.buildMenuHtmlBuilder(appMenuList, currentUser, request, response, ViewManager.htmlView, qzRequest.getTargetType(), curTargetName, qzRequest.getAppName(), qzRequest.getModelName()));
            %>
        </ul>
    </div>

    <div class="menu-toggle-btn">
        <a href="javascript:void(0);" data-toggle="push-menu">
            <i class="icon icon-sliders"></i>
        </a>
    </div>
</aside>
