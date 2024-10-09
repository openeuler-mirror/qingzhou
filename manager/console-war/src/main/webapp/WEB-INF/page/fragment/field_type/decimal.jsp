<%@ page pageEncoding="UTF-8" %>

<input type="number" step="0.1" min="<%=modelField.getMin()%>" max="<%=modelField.getMax()%>"
	   name="<%=fieldName%>" value='<%=fieldValue%>' class="form-control" <%=readonly%>>