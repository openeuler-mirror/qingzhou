<%@ page pageEncoding="UTF-8" %>

<%
	String selectVal = "";
	StringBuilder selectHtml = new StringBuilder("<ul class=\"list\">");
	selectHtml.append("<li data-value=\"\" class=\"option\"></li>");
	String inputParam = request.getParameter(fieldName);
	for (String option : modelOptionsEntry) {
		boolean setSelect = Objects.equals(inputParam, option);
		if (setSelect) {
			selectVal = option;
			selectHtml.append("<li data-value=\"").append(option).append("\" class=\"option selected focus\">").append(option).append("</li>");
		} else {
			selectHtml.append("<li data-value=\"").append(option).append("\" class=\"option\">").append(option).append("</li>");
		}
	}
	selectHtml.append("</ul>");

	selectHtml.insert(0, "<input type=\"text\" name=\"" + fieldName + "\" value=\"" + selectVal + "\" placeholder=\"" + I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName) + "\" >");
	selectHtml = new StringBuilder("<div class=\"form-control nice-select wide\">" + selectHtml + "</div>");

	out.print(selectHtml.toString());
%>
