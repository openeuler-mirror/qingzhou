<%@ page pageEncoding="UTF-8" %>

<input type="number" step="0.1"
	   placeholder="<%=isForm?modelField.getPlaceholder():(modelField.getPlaceholder().isEmpty()?I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName):modelField.getPlaceholder())%>"
	   min="<%=modelField.getMin()%>" max="<%=modelField.getMax()%>"
	   name="<%=fieldName%>" value='<%=fieldValue%>' <%=echoGroup%> class="form-control">