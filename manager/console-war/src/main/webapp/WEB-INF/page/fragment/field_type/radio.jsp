<%@ page pageEncoding="UTF-8" %>

<%
    if (!readonly.isEmpty()) {
        readonly = " onclick='return false;' readonly";
    }

    for (ItemInfo itemInfo : SystemController.getOptions(qzApp, modelInfo, fieldName)) {
        String option = itemInfo.getName();
%>
<label class="radio-inline radio-label radio-anim">
    <input type="radio" name="<%=fieldName%>"
           value='<%=option%>' <%=Objects.equals(fieldValue, option) ? "checked" : ""%> <%=readonly%>>
    <i class="radio-i"></i> <%=I18n.getStringI18n(itemInfo.getI18n())%>
</label>
<%
    }
%>