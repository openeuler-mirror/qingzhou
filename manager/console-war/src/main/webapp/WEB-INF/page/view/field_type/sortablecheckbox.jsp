<%@ page pageEncoding="UTF-8" %>

<%
	if (!readonly.isEmpty()) {
		readonly = " onclick='return false;' readonly";
	}
%>
<div class="checkbox-group sortable">
	<%
		String[] options = SystemController.getOptions(qzApp, modelInfo.getModelFieldInfo(fieldName));
		Arrays.sort(options, Comparator.comparingInt(fieldValues::indexOf));
		for (String option : options) {
	%>
	<a draggable="true" href="javascript:void(0);">
		<input type="checkbox" name="<%=fieldName%>"
			   value='<%=option%>' <%=(fieldValues.contains(option) ? "checked" : "")%> <%=readonly%> />
		<label><%=option%>
		</label>
	</a>
	<%
		}

	%>
</div>
