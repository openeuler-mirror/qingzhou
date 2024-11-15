<%@ page pageEncoding="UTF-8" %>

<%
    for (ItemInfo itemInfo : SystemController.getOptions(qzRequest, fieldName)) {
        String option = itemInfo.getName();
%>
<label class="radio-inline radio-label radio-anim">
    <input type="radio" name="<%=fieldName%>"
           value='<%=option%>' <%=echoGroup%> <%=Objects.equals(fieldValue, option) ? "checked" : ""%>>
    <i class="radio-i"></i> <%=I18n.getStringI18n(itemInfo.getI18n())%>
</label>
<%
    }
%>