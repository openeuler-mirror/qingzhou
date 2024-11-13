<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
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

	<%@ include file="../fragment/back.jsp" %>
</div>
