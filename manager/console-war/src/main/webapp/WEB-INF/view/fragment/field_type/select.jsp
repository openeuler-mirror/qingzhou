<%@ page pageEncoding="UTF-8" %>

<%
    int index = 0;
    int tabIndex = 0;
    String selectVal = "";
    String selectText = "";
    String selectHtml = "<ul class=\"list\">";

    if (Objects.equals(fieldValue, "")) {
        selectVal = "";
        selectText = "";
        selectHtml += "<li data-value=\"\" class=\"option selected focus\" format=\"\"></li>";
    } else {
        selectHtml += "<li data-value=\"\" class=\"option\" format=\"\"></li>";
    }
    boolean selected = false;
    for (ItemInfo itemInfo : SystemController.getOptions(qzApp, modelInfo, fieldName)) {
        String option = itemInfo.getName();
        //处理option当中有双引号
        String optionHtmlStr = option.replaceAll("\"", "&quot;");
        String colorStyle = SystemController.getColorStyle(modelInfo, fieldName, option);

        String optionI18n = I18n.getStringI18n(itemInfo.getI18n());
        if (Objects.equals(fieldValue, option)) {
            selected = true;
            selectVal = optionHtmlStr;
            selectText = optionI18n;
            tabIndex = index;
            selectHtml += "<li style=\"" + colorStyle + "\" data-value=\"" + optionHtmlStr + "\" class=\"option selected focus\" format=\"" + optionHtmlStr + "\">" + optionI18n + "</li>";
        } else {
            selectHtml += "<li style=\"" + colorStyle + "\" data-value=\"" + optionHtmlStr + "\" class=\"option\" format=\"" + optionHtmlStr + "\">" + optionI18n + "</li>";
        }
        index++;
    }

    selectHtml += "</ul>";

    if (!selected) {
        selectVal = fieldValue;
        selectText = fieldValue;
    }

    String showText = ((selectText == null || "".equals(selectText.trim())) ? selectVal : selectText);
    selectHtml = "<input type=\"text\" value=\"" + selectText + "\" text=\"" + selectText + "\"" + echoGroup + " autocomplete=\"off\" style=\"background-color: rgba(0, 0, 0, 0);\"  placeholder=\"" + I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName) + "\">"
            + "<input type=\"hidden\" name=\"" + fieldName + "\" value=\"" + selectVal + "\"" + " format=\"" + showText + "\">" + selectHtml;
    selectHtml = "<div class=\"form-control nice-select wide\" style='height: 32px' tabindex=\"" + tabIndex + "\">" + selectHtml + "</div>";

    out.print(selectHtml);
%>
