<%@ page pageEncoding="UTF-8" %>

<%
	if (!"".equals(readonly)) {
		readonly = " onclick='return false;' readonly";
	}

	for (String option : SystemController.getOptions(qzApp, modelInfo.getModelFieldInfo(fieldName))) {
%>
<label class="checkbox-inline checkbox-label checkbox-anim">
	<input type="checkbox" name="<%=fieldName%>"
		   value='<%=option%>' <%=(fieldValues.contains(option) ? "checked" : "")%> <%=readonly%>>
	<i class="checkbox-i"></i> <%=option%>
</label>
<%
	}
%>