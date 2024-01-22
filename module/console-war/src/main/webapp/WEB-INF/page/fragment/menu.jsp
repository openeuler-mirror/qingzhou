<%@ page pageEncoding="UTF-8" %>

<%-- 左侧菜单 --%>
<aside class="main-sidebar">
    <div class="sidebar sidebar-scroll">
        <ul class="sidebar-menu" data-widget="tree">
            <%
            List<MenuItem> appMenuList = PageBackendService.getAppMenuList(currentUser, qzRequest.getAppName());
            out.print(PageBackendService.buildMenuHtmlBuilder(appMenuList, currentUser, request, response, ViewManager.htmlView, qzRequest.getAppName(), qzRequest.getModelName()));
            %>
        </ul>
    </div>

    <div class="menu-toggle-btn">
        <a href="javascript:void(0);" data-toggle="push-menu">
            <i class="icon icon-sliders"></i>
        </a>
    </div>
</aside>
