<%@ page pageEncoding="UTF-8" %>

<form name="filterForm" id="filterForm" method="POST"
      action="<%=RESTController.encodeURL( response, ViewManager.htmlView + "/" + qzApp + "/" + qzModel + "/" + qingzhou.api.type.List.ACTION_LIST)%>">
    <div class="row filterForm" style="margin-top: 10px;">
        <%
            for (String fieldName : modelInfo.getFieldsToListSearch()) {
                ItemInfo[] fieldOptions = SystemController.getOptions(qzApp, modelInfo, fieldName);
        %>
        <div class='col-md-2 col-sm-3 col-xs-4 list-page-padding-bottom'>
            <div class="input-control">
                <%
                    if (fieldOptions != null && fieldOptions.length != 0) {
                        String selectVal = "";
                        StringBuilder selectHtml = new StringBuilder("<ul class=\"list\">");
                        selectHtml.append("<li data-value=\"\" class=\"option\"></li>");
                        String inputParam = request.getParameter(fieldName);
                        for (ItemInfo itemInfo : fieldOptions) {
                            String option = itemInfo.getName();
                            String optionI18n = I18n.getStringI18n(itemInfo.getI18n());
                            boolean setSelect = Objects.equals(inputParam, option);
                            if (setSelect) {
                                selectVal = option;
                                selectHtml.append("<li data-value=\"").append(option).append("\" class=\"option selected focus\">").append(optionI18n).append("</li>");
                            } else {
                                selectHtml.append("<li data-value=\"").append(option).append("\" class=\"option\">").append(optionI18n).append("</li>");
                            }
                        }
                        selectHtml.append("</ul>");

                        selectHtml.insert(0, "<input type=\"text\" name=\"" + fieldName + "\" value=\"" + selectVal + "\" placeholder=\"" + I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName) + "\" >");
                        selectHtml = new StringBuilder("<div class=\"form-control nice-select wide\">" + selectHtml + "</div>");

                        out.print(selectHtml.toString());
                    } else {
                        String showHtml = request.getParameter(fieldName);
                        if (showHtml == null) {
                            showHtml = "";
                        }
                %>
                <input id="<%=fieldName%>" type="text" name="<%=fieldName%>" value='<%=showHtml%>' class="form-control"
                       placeholder="<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>">
                <%
                    }
                %>
            </div>
        </div>
        <%
            }
        %>
        <div class="col-md-2 col-sm-3 col-xs-4 search-btn" style="margin-bottom: 16px;">
            <span class="input-group-btn col-md-4" style="width: 18%;padding-left:0;">
                <a class="btn"
                   href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, qingzhou.api.type.List.ACTION_LIST)%>"
                   form="filterForm">
                    <i class="icon icon-search"></i> <%=I18n.getKeyI18n("page.filter")%>
                </a>
            </span>
        </div>
    </div>
</form>