<%@ page pageEncoding="UTF-8" %>

<%
    if (!"".equals(readonly)) {
        readonly = " onclick='return false;' readonly";
    }

    for (ItemInfo itemInfo : SystemController.getOptions(qzApp, modelInfo, fieldName)) {
        String option = itemInfo.getName();
%>
<label class="checkbox-inline checkbox-label checkbox-anim">
    <input type="checkbox" name="<%=fieldName%>"
           value='<%=option%>' <%=(fieldValues.contains(option) ? "checked" : "")%> <%=readonly%>>
    <i class="checkbox-i"></i> <%=I18n.getStringI18n(itemInfo.getI18n())%>
</label>
<%
    }
%>