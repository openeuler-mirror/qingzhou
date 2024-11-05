<%@ page pageEncoding="UTF-8" %>

<%-- 左侧菜单 --%>
<aside class="main-sidebar" bindingId="<%=randBindingId%>">
    <div class="sidebar sidebar-scroll">
        <ul class="sidebar-menu" data-widget="tree">
            <%
                out.print(PageUtil.buildMenu(request, response, qzRequest));
            %>
        </ul>
    </div>

    <div class="menu-toggle-btn">
        <a href="javascript:void(0);" data-toggle="push-menu">
            <i class="icon icon-sliders"></i>
        </a>
    </div>
</aside>

<section class="main-body" bindingId="<%=randBindingId%>">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <%-- 首页面主体部分 --%>
    <div class="bodyDiv" bindingId="<%=randBindingId%>">
        <%
            Map<String, String> infoData = qzResponse.getDataMap();
        %>
        <%@ include file="../fragment/info.jsp" %>
    </div>
</section>