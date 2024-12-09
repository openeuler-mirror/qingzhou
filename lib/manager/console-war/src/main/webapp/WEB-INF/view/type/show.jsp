<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<%
		Map<String, String> infoData = (Map) qzResponse.getInternalData();
	%>
	<%@ include file="../fragment/info.jsp" %>

	<div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
		<div class="form-btn">
			<%
				for (String action : modelInfo.getShowActions()) {
					if (!SecurityController.isActionPermitted(qzApp, qzModel, action, request, infoData)) continue;
					ModelActionInfo modelActionInfo = modelInfo.getModelActionInfo(action);
					String dataTip = I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + action);
					if (Utils.isBlank(dataTip)) {
						dataTip = I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + action);
					}
			%>
			<a class="btn" data-tip-arrow="top" action-id="<%=qzApp + "-" + qzModel + "-" + action%>"
			   data-tip='<%=dataTip%>'
			   action-type="<%=modelActionInfo.getActionType()%>"
			   href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, action)%>"
			>
				<i class="icon icon-<%=modelActionInfo.getIcon()%>"></i>
				<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + action)%>
			</a>
			<%
				}

				if (SecurityController.isActionPermitted(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, request)) {
			%>
			<a modelname="<%=qzModel%>" class="btn"
			   href="javascript:void(0);" onclick="returnHref(this);">
				<i class="icon icon-reply"></i>
				<%=I18n.getKeyI18n("page.return")%>
			</a>
			<%
				}
			%>
		</div>
	</div>
</div>
