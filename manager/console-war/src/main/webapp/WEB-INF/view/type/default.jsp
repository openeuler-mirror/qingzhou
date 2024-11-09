<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv" bindingId="<%=randBindingId%>">
	<%-- 面包屑分级导航 --%>
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<table class="table table-striped table-hover">
		<tr>
			<td>
				<%=I18n.getKeyI18n("page.status")%>:
			</td>
			<td>
				<%=qzResponse.isSuccess()%>
			</td>
		</tr>
		<tr>
			<td>
				<%=I18n.getKeyI18n("page.msg")%>:
			</td>
			<td>
				<%=qzResponse.getMsg()%>
			</td>
		</tr>
	</table>

	<%
		if (SecurityController.isActionPermitted(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, currentUser)) {
	%>
	<div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
		<div class="form-btn">
			<a href="javascript:void(0)" class="btn" onclick="returnHref(this);">
				<%=I18n.getKeyI18n("page.return")%>
			</a>
		</div>
	</div>
	<%
		}
	%>
</div>