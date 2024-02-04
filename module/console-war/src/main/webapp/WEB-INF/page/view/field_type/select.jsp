<%@ page pageEncoding="UTF-8" %>

<%
if (readonly == null) {
    return; // for 静态源码漏洞扫描
}
if (model == null) {
    return; // for 静态源码漏洞扫描
}

if (!"".equals(readonly)) {
    readonly = "onfocus='this.defaultIndex=this.selectedIndex;' onchange='this.selectedIndex=this.defaultIndex;' readonly";
}
int index = 0;
int tabIndex = 0;
String selectVal = "";
String selectText = "";
String selectHtml = "<ul class=\"list\">";

Options optionManager = modelManager.getOptions(qzRequest.getModelName(), fieldName);
if (optionManager != null) {
    for (Option option : optionManager.options()) {
        String val = option.value();
        String name = I18n.getString(option.i18n());
        if (Objects.equals(fieldValue, val)) {
            selectVal = val;
            selectText = name;
            tabIndex = index;
            selectHtml += "<li data-value=\"" + val + "\" class=\"option selected focus\" format=\"" + name + "\">" + name + "</li>"; // TODO options.getFormat(name) ?
        } else {
            selectHtml += "<li data-value=\"" + val + "\" class=\"option\" format=\"" + name + "\">" + name + "</li>";
        }
        index++;
    }
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
