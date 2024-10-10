<%@ page pageEncoding="UTF-8" %>

<%
    if (!readonly.isEmpty()) {
        readonly = " onclick='return false;' readonly";
    }
%>
<div class="checkbox-group sortable">
    <%
        for (ItemInfo itemInfo : SystemController.getOptions(qzApp, modelInfo, fieldName)) {
            String option = itemInfo.getName();
    %>
    <a draggable="true" href="javascript:void(0);">
        <input type="checkbox" name="<%=fieldName%>"
               value='<%=option%>' <%=(fieldValues.contains(option) ? "checked" : "")%> <%=readonly%> />
        <label><%=I18n.getStringI18n(itemInfo.getI18n())%>
        </label>
    </a>
    <%
        }

    %>
</div>
