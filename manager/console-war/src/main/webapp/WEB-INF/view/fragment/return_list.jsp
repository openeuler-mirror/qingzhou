<%@ page pageEncoding="UTF-8" %>

<%
	String modelName = PageUtil.getBackModel(session, qzRequest);
%>
<div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
	<div class="form-btn">
	<a action-type="<%=BackFilter.BACK_URI%>" class="btn"  <%=modelName==null?"disabled='disabled'":""%> modelname="<%=modelName%>"
	   href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, BackFilter.BACK_URI)%>">
		<%=I18n.getKeyI18n("page.return")%>
	</a>
	</div>
</div>