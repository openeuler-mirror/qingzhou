<%@ page pageEncoding="UTF-8" %>

<%
String selectVal = "";
String selectHtml = "<ul class=\"list\">";
selectHtml += "<li data-value=\"\" class=\"option\"></li>";
for (Option option : modelOptionsEntry) {
    String val = option.value();
    String name = I18n.getString(option.i18n());
    if (StringUtil.isBlank(val)) {
        continue;
    }
    String param = request.getParameter(fieldName);
    boolean setSelect = false;
    if(!Objects.equals(param, val) &&
            Objects.equals(param, name)){
        setSelect = true;
    }
    if(Objects.equals(param, val) || setSelect) {
        selectVal = val;
        selectHtml += "<li data-value=\"" + val + "\" class=\"option selected focus\">" + name + "</li>";
    } else {
        selectHtml += "<li data-value=\"" + val + "\" class=\"option\">" + name + "</li>";
    }
}
selectHtml += "</ul>";

selectHtml = "<input type=\"text\" name=\"" + fieldName + "\" value=\"" + selectVal + "\" placeholder=\"" + I18n.getString(menuAppName, "model.field." + qzRequest.getModelName() + "." + fieldName) +  "\" >" + selectHtml;
selectHtml = "<div class=\"form-control nice-select wide\">" + selectHtml + "</div>";

out.print(selectHtml);
%>
