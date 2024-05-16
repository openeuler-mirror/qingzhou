<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    if (qzRequest == null || qzResponse == null || modelInfo == null) {
        return; // for 静态源码漏洞扫描
    }
    final boolean hasId = PageBackendService.hasIDField(qzRequest);
    if (!qzResponse.getDataList().isEmpty()) {
%>
<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <%@ include file="../fragment/info.jsp" %>
</div>
<%
    }
%>