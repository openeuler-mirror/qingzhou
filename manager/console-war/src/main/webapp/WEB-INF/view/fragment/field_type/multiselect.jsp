<%@ page pageEncoding="UTF-8" %>

<%
    if (!readonly.isEmpty()) {
        readonly = "onfocus='this.defaultIndex=this.selectedIndex;' onchange='this.selectedIndex=this.defaultIndex;' readonly";
    }
%>
<select name="<%=fieldName%>" multiple="multiple" <%=readonly%> style="width:100%;">
    <%
        for (ItemInfo itemInfo : SystemController.getOptions(qzApp, modelInfo, fieldName)) {
            String option = itemInfo.getName();
    %>
    <option value='<%=option%>' <%=fieldValues.contains(option) ? "selected" : ""%>>
        <%=I18n.getStringI18n(itemInfo.getI18n())%>
    </option>
    <%
        }
    %>
</select>