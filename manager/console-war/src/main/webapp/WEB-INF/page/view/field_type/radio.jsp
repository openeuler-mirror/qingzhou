<%@ page pageEncoding="UTF-8" %>

<%
    if (!readonly.isEmpty()) {
        readonly = " onclick='return false;' readonly";
    }

    String[] radioOptions = modelInfo.getFieldOptions(fieldName);
    for (String option : radioOptions) {
%>
<label class="radio-inline radio-label radio-anim">
    <input type="radio" name="<%=fieldName%>"
           value='<%=option%>' <%=Objects.equals(fieldValue, option) ? "checked" : ""%> <%=readonly%>>
    <i class="radio-i"></i> <%=option%>
</label>
<%
    }
%>