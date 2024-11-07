<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<%
		String[] fieldsToListSearch = modelInfo.getFieldsToListSearch();
		if (fieldsToListSearch.length > 0) {
	%>
	<%@ include file="../fragment/filter_form.jsp" %>
	<hr style="margin-top: 4px;">
	<%
		}
		String url = PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG, qzAction + (Utils.notBlank(encodedId) ? "/" + encodedId : ""));
	%>

	<div class="infoPage" chart="true" data-url="<%=url%>" xAxisField="">
		<div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
			<div class="panel-body" style="word-break: break-all">
				<div container="chart" style="height: 600px;width: 100%;"></div>
			</div>
		</div>
	</div>

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
