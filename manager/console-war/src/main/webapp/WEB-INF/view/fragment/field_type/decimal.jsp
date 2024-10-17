<%@ page pageEncoding="UTF-8" %>

<input type="number" step="0.1" min="<%=modelField.getMin()%>" max="<%=modelField.getMax()%>"
	   name="<%=fieldName%>" value='<%=fieldValue%>' <%=echoGroup%> class="form-control" <%=readonly%>>