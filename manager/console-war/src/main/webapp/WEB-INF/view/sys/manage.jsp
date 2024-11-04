<%@ page pageEncoding="UTF-8" %>

<%@ include file="../fragment/head.jsp" %>

<%-- 初始化管理后的菜单页面 --%>
<%@ include file="../fragment/menu.jsp" %>

<section class="main-body">
	<%-- 面包屑分级导航 --%>
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<%-- 首页面主体部分 --%>
	<div class="bodyDiv">
		<%
			Map<String, String> infoData = qzResponse.getDataMap();
		%>
		<%@ include file="../fragment/info.jsp" %>
	</div>
</section>
