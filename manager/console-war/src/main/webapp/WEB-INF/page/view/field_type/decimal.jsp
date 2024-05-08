<%@ page pageEncoding="UTF-8" %>

<%
    if (modelField == null) {
        return; // for 静态源码漏洞扫描
    }
%>

<input type="number" step="0.1" min="<%=modelField.getMin()%>" max="<%=modelField.getMax()%>"
       name="<%=fieldName%>" value='<%=fieldValue%>' class="form-control" <%=readonly%>>