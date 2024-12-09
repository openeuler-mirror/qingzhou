<%@ page pageEncoding="UTF-8" %>

<%
	if (SecurityController.isActionPermitted(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, request)) {
%>
<div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
	<div class="form-btn">
		<a modelname="<%=qzModel%>" class="btn"
		   href="javascript:void(0);" onclick="returnHref(this);">
			<i class="icon icon-reply"></i>
			<%=I18n.getKeyI18n("page.return")%>
		</a>
	</div>
</div>
<%
	}
%>
