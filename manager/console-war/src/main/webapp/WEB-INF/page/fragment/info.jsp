<%@ page pageEncoding="UTF-8" %>

<div class="infoPage">
	<div class="block-bg">
		<%
			List<Map<String, String>> dataList = qzResponse.getDataList();
			Map<String, String> infoData = dataList.get(0);
		%>
		<%@ include file="field.jsp" %>
	</div>
</div>

<%
	if (request.getAttribute("comeFromIndexPage") == null) {
%>
<div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
	<div class="form-btn">
		<a href="javascript:void(0);" onclick="tw.goback(this);" btn-type="goback" class="btn">
			<%=I18n.getKeyI18n("page.return")%>
		</a>
	</div>
</div>
<%
	}
%>