<%@ page pageEncoding="UTF-8" %>

<%
    for (ItemInfo itemInfo : SystemController.getOptions(qzApp, modelInfo, fieldName)) {
        String option = itemInfo.getName();
%>
<label class="checkbox-inline checkbox-label checkbox-anim">
    <input type="checkbox" name="<%=fieldName%>"
           value='<%=option%>' <%=echoGroup%> <%=(fieldValues.contains(option) ? "checked" : "")%>>
    <i class="checkbox-i"></i> <%=I18n.getStringI18n(itemInfo.getI18n())%>
</label>
<%
    }
%>