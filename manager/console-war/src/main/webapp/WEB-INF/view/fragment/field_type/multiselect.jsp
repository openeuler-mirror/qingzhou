<%@ page pageEncoding="UTF-8" %>

<select name="<%=fieldName%>" multiple="multiple" style="width:100%;">
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