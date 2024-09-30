<%@ page pageEncoding="UTF-8" %>
<%
	if (!"".equals(readonly)) {
		readonly = " disallowed";
	}
%>
<div class="switch-btn<%=readonly%>">
	<div class="switchedge <%="true".equals(fieldValue) ? "switch-bg":""%>">
		<div class="circle <%="true".equals(fieldValue) ? "switch-right":""%>"></div>
	</div>
	<input type="hidden" name="<%=fieldName%>" value='<%=fieldValue%>'>
</div>
