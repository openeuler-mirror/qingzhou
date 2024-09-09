<%@ page pageEncoding="UTF-8" %>

<%
	if (!readonly.isEmpty()) {
		readonly = "onfocus='this.defaultIndex=this.selectedIndex;' onchange='this.selectedIndex=this.defaultIndex;' readonly";
	}
%>
<select name="<%=fieldName%>" multiple="multiple" <%=readonly%> style="width:100%;">
	<%
		String[] multiOptions = modelInfo.getFieldOptions(fieldName);
		for (String option : multiOptions) {
	%>
	<option value='<%=option%>' <%=fieldValues.contains(option) ? "selected" : ""%>>
		<%=option%>
	</option>
	<%
		}
	%>
</select>