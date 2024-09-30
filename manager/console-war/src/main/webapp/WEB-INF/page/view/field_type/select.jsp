<%@ page pageEncoding="UTF-8" %>

<%
	if (!readonly.isEmpty()) {
		readonly = "onfocus='this.defaultIndex=this.selectedIndex;' onchange='this.selectedIndex=this.defaultIndex;' readonly";
	}
	int index = 0;
	int tabIndex = 0;
	String selectVal = "";
	String selectText = "";
	String selectHtml = "<ul class=\"list\">";

	for (String option : SystemController.getOptions(qzApp, modelInfo.getModelFieldInfo(fieldName))) {
		if (Objects.equals(fieldValue, option)) {
			selectVal = option;
			selectText = option;
			tabIndex = index;
			selectHtml += "<li data-value=\"" + option + "\" class=\"option selected focus\" format=\"" + option + "\">" + option + "</li>";
		} else {
			selectHtml += "<li data-value=\"" + option + "\" class=\"option\" format=\"" + option + "\">" + option + "</li>";
		}
		index++;
	}

	selectHtml += "</ul>";

	String showText = ((selectText == null || "".equals(selectText.trim())) ? selectVal : selectText);
	if (index <= 6) {
		selectHtml = "<span>" + showText + "</span><input type=\"hidden\" name=\"" + fieldName + "\" value=\"" + selectVal + "\"" + readonly + " format=\"" + showText + "\">" + selectHtml;
	} else {
		selectHtml = "<input type=\"text\" value=\"" + selectText + "\" text=\"" + selectText + "\"" + readonly + " autocomplete=\"off\">"
				+ "<input type=\"hidden\" name=\"" + fieldName + "\" value=\"" + selectVal + "\"" + readonly + " format=\"" + showText + "\">" + selectHtml;
	}
	selectHtml = "<div class=\"form-control nice-select wide\" tabindex=\"" + tabIndex + "\">" + selectHtml + "</div>";

	out.print(selectHtml);
%>
