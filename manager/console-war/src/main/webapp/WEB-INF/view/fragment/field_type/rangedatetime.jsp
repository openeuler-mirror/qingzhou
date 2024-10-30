<%@ page import="java.text.SimpleDateFormat" %>
<%@ page pageEncoding="UTF-8" %>
<%
    String formatDate = "";
    for (int i = 0; i < 2; i++) {
        if (fieldValues.size() >= i + 1 && !fieldValues.get(i).isEmpty()) {
            formatDate = fieldValues.get(i);
        }
        if (i == 1){
            out.print("<span class=\"separator\" style=\"\n" +
                    "    display: flex;\n" +
                    "    align-items: center;\n" +
                    "\">一</span>");
        }
%>
<input type="text" name="<%=fieldName%>" value='<%=formatDate%>'"
       autocomplete="off"
       placeholder="<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>"
       class="form-control form-datetime"
       data-date-format="yyyy-mm-dd hh:ii:ss" <%--须保持一致：DeployerConstants.FIELD_DATETIME_FORMAT--%>
       data-min-view="0"
       data-minute-step="3"
       data-date-language="<%= I18n.isZH() ? "zh-cn" : "en" %>">
<%
    }
%>