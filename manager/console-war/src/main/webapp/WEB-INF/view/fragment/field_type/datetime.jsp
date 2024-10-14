<%@ page import="java.text.SimpleDateFormat" %>
<%@ page pageEncoding="UTF-8" %>

<%
    Date date = new Date();
    if (Utils.notBlank(fieldValue) && !fieldValue.equals("0")) {
        date.setTime(Long.parseLong(fieldValue));
        fieldValue = new SimpleDateFormat(DeployerConstants.FIELD_DATETIME_FORMAT).format(date);
    } else {
        fieldValue = "";
    }
%>
<input type="text" name="<%=fieldName%>" value='<%=fieldValue%>'
       autocomplete="off"
       class="form-control form-datetime"
       data-date-format="yyyy-mm-dd hh:ii:ss" <%--须保持一致：DeployerConstants.FIELD_DATETIME_FORMAT--%>
       data-min-view="0"
       data-minute-step="3"
       data-date-language="<%= I18n.isZH() ? "zh-cn" : "en" %>">
