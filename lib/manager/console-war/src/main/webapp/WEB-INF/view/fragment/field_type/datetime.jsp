<%@ page pageEncoding="UTF-8" %>

<%
    {
        String fieldValueForFile;
        if (Utils.notBlank(fieldValue) && !fieldValue.equals("0")) {
            Date date = new Date();
            date.setTime(Long.parseLong(fieldValue));
            fieldValueForFile = new SimpleDateFormat(DeployerConstants.DATETIME_FORMAT).format(date);
        } else {
            fieldValueForFile = "";
        }
%>
<input type="text" name="<%=fieldName%>" value='<%=fieldValueForFile%>' <%=echoGroup%>
       placeholder="<%=isForm?modelField.getPlaceholder():(modelField.getPlaceholder().isEmpty()?I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName):modelField.getPlaceholder())%>"
       autocomplete="off"
       class="form-control form-datetime"
       data-date-format="yyyy-mm-dd hh:ii:ss" <%--须保持一致：DeployerConstants.FIELD_DATETIME_FORMAT--%>
       data-min-view="0"
       data-minute-step="3"
       data-date-language="<%= I18n.isZH() ? "zh-cn" : "en" %>">
<%
    }
%>