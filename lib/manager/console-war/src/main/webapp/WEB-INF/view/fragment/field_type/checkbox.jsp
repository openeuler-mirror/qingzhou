<%@ page pageEncoding="UTF-8" %>

<%
    for (ItemData itemData : SystemController.getOptions(qzRequest, fieldName)) {
        String option = itemData.getName();
%>
<label class="checkbox-inline checkbox-label checkbox-anim">
    <input type="checkbox" name="<%=fieldName%>"
           value='<%=option%>' <%=echoGroup%> <%=(fieldValues.contains(option) ? "checked" : "")%>>
    <i class="checkbox-i"></i> <%=I18n.getStringI18n(itemData.getI18n())%>
</label>
<%
    }
%>