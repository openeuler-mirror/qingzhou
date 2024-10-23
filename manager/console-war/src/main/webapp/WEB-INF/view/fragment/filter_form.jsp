<%@ page pageEncoding="UTF-8" %>

<%
    if (fieldsToListSearch.length > 0) {
%>

<form name="filterForm" class="filterForm" method="POST"
      action="<%=RESTController.encodeURL( response, HtmlView.FLAG + "/" + qzApp + "/" + qzModel + "/" + qingzhou.api.type.List.ACTION_LIST)%>">
    <div class="row" style="margin-top: 10px;">
        <%
            for (String fieldName : fieldsToListSearch) {
        %>
        <div class='col-md-2 col-sm-3 col-xs-4 list-page-padding-bottom'>
            <div class="input-control">
                <%
                    String showHtml = request.getParameter(fieldName);
                    if (showHtml == null) {
                        Map<String, String> searchParameters = modelInfo.getFilterValues();
                        if (searchParameters != null) {
                            showHtml = searchParameters.get(fieldName);
                        }
                    }
                    if (showHtml == null) {
                        showHtml = "";
                    }
                %>
                <input id="<%=fieldName%>" type="text" name="<%=fieldName%>" value='<%=showHtml%>' class="form-control"
                       placeholder="<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>">
            </div>
        </div>
        <%
            }
        %>
        <div class="col-md-2 col-sm-3 col-xs-4 search-btn" style="margin-bottom: 16px;">
            <span class="input-group-btn col-md-4" style="width: 18%;padding-left:0;">
                <a class="btn filter_search"
                   href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, qingzhou.api.type.List.ACTION_LIST)%>">
                    <i class="icon icon-search"></i> <%=I18n.getKeyI18n("page.filter")%>
                </a>
            </span>

            <span class="input-group-btn col-md-4" style="width: 18%;padding-left:0;margin-left: 45px;">
                <a class="btn"
                   href="javascript:void(0);" onclick="filter_reset(this);">
                    <i class="icon icon-search"></i> <%=I18n.getKeyI18n("page.filter.reset")%>
                </a>
            </span>
        </div>
    </div>
</form>

<%
    }
%>
