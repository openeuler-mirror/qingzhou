<%@ page import="qingzhou.api.type.Dashboard" %>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <%
        String url = PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG, Dashboard.ACTION_DASHBOARD + (Utils.notBlank(encodedId) ? "/" + encodedId : ""));
    %>

    <div class="dashboardPage" data-url="<%=url%>">
    </div>
</div>