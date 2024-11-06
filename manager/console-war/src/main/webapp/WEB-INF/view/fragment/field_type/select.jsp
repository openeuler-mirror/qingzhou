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
        String[] color = SystemController.getColor(modelInfo, fieldName);
        String colorStyle = "";
        if (color != null){
            for (String condition : color) {
                String[] array = condition.split(":");
                if (array.length != 2) {
                    continue;
                }
                if (array[0].equals(option)) {
                    colorStyle = "color:" + array[1];
                    break;
                }
            }
        }
        String optionI18n = I18n.getStringI18n(itemInfo.getI18n());
        if (Objects.equals(fieldValue, option)) {
            selected = true;
            selectVal = option;
            selectText = optionI18n;
            tabIndex = index;
            selectHtml += "<li style=\""+ colorStyle +"\" data-value=\"" + option + "\" class=\"option selected focus\" format=\"" + option + "\">" + optionI18n + "</li>";
        } else {
            selectHtml += "<li style=\""+ colorStyle +"\" data-value=\"" + option + "\" class=\"option\" format=\"" + option + "\">" + optionI18n + "</li>";
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
            + "<input type=\"hidden\" name=\"" + fieldName + "\" value=\"" + selectVal + "\"" + echoGroup + " format=\"" + showText + "\">" + selectHtml;
    selectHtml = "<div class=\"form-control nice-select wide\" tabindex=\"" + tabIndex + "\">" + selectHtml + "</div>";

    out.print(selectHtml);
%>
