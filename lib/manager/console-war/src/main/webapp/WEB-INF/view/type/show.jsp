<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<%
		Map<String, String> infoData = (Map) qzResponse.getInternalData();
	%>
	<%@ include file="../fragment/info.jsp" %>
	<%@ include file="../fragment/back.jsp" %>
</div>
