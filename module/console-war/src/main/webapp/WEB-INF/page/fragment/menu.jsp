<%@ page pageEncoding="UTF-8" %>

<%-- 左侧菜单 --%>
<aside class="main-sidebar">
    <div class="sidebar sidebar-scroll">
        <ul class="sidebar-menu" data-widget="tree">
            <%
                // 菜单 TODO 当前特殊处理了，需要做到点默认实例时动态切换
                String curAppName = Constants.QINGZHOU_MASTER_APP_NAME.equals(qzRequest.getTargetName()) ? Constants.QINGZHOU_MASTER_APP_NAME : ConsoleUtil.getAppName(qzRequest.getTargetType(),qzRequest.getTargetName());
                List<Properties> appMenuList = ConsoleUtil.getAppMenuList(currentUser, curAppName);
                String curTargetName = Constants.QINGZHOU_MASTER_APP_NAME.equals(qzRequest.getTargetName()) ? Constants.QINGZHOU_MASTER_APP_NAME : qzRequest.getTargetName();
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
