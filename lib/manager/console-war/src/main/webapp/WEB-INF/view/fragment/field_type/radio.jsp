<%@ page pageEncoding="UTF-8" %>

<%
    for (ItemData itemData : SystemController.getOptions(qzRequest, fieldName)) {
        String option = itemData.getName();
%>
<label class="radio-inline radio-label radio-anim">
    <input type="radio" name="<%=fieldName%>"
           value='<%=option%>' <%=echoGroup%> <%=Objects.equals(fieldValue, option) ? "checked" : ""%>>
    <i class="radio-i"></i> <%=I18n.getStringI18n(itemData.getI18n())%>
</label>
<%
    }
%>