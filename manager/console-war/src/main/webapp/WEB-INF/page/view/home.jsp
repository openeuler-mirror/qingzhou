<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
	if (qzResponse.getDataList().isEmpty()) return;
%>

<div class="bodyDiv">
	<%-- 面包屑分级导航 --%>
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<%@ include file="../fragment/info.jsp" %>
</div>