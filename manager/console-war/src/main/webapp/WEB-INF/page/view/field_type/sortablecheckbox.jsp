<%@ page pageEncoding="UTF-8" %>

<%
    if (!readonly.isEmpty()) {
        readonly = " onclick='return false;' readonly";
    }
%>
<div class="checkbox-group sortable">
    <%
        for (String option : SystemController.getOptions(qzApp, modelInfo.getModelFieldInfo(fieldName))) {
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
