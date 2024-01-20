<%@ page pageEncoding="UTF-8" %>

<input type="text" name="<%=fieldName%>" value='<%=fieldValue%>'
       autocomplete="off"
       class="form-control form-datetime"
       data-date-format="yyyy-mm-dd hh:ii:ss"
       data-min-view="0"
       data-minute-step="3"
       data-date-language="<%= I18n.isZH() ? "zh-cn" : "en" %>">
