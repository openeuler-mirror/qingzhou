<%@ page pageEncoding="UTF-8" %>

<%
    String selectVal = "";
    String selectHtml = "<ul class=\"list\">";
    selectHtml += "<li data-value=\"\" class=\"option\"></li>";
    for (String option : modelOptionsEntry) {
        if (option == null) {
            continue;
        }
        if (option.trim().isEmpty()) {
            continue;
        }

        String param = request.getParameter(fieldName);
        boolean setSelect = false;
        if (!Objects.equals(param, option) &&
                Objects.equals(param, option)) {
            setSelect = true;
        }
        if (Objects.equals(param, option) || setSelect) {
            selectVal = option;
            selectHtml += "<li data-value=\"" + option + "\" class=\"option selected focus\">" + option + "</li>";
        } else {
            selectHtml += "<li data-value=\"" + option + "\" class=\"option\">" + option + "</li>";
        }
    }
    selectHtml += "</ul>";

    selectHtml = "<input type=\"text\" name=\"" + fieldName + "\" value=\"" + selectVal + "\" placeholder=\"" + I18n.getString(menuAppName, "model.field." + qzRequest.getModel() + "." + fieldName) + "\" >" + selectHtml;
    selectHtml = "<div class=\"form-control nice-select wide\">" + selectHtml + "</div>";

    out.print(selectHtml);
%>
