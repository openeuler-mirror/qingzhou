<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<%
		Map<String, String> infoData = qzResponse.getDataMap();
	%>
	<%@ include file="../fragment/info.jsp" %>

	<%
		if (SecurityController.isActionPermitted(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, currentUser)) {
	%>
	<div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
		<div class="form-btn">
			<a class="btn"
			   onclick="returnHref('<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, qingzhou.api.type.List.ACTION_LIST)%>')"
			   href="javascript:void(0)">
				<%=I18n.getKeyI18n("page.return")%>
			</a>
		</div>
	</div>
	<%
		}
	%>
</div>