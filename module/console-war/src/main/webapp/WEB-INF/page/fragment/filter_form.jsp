<%@ page pageEncoding="UTF-8" %>

<form name="filterForm" id="filterForm" method="POST"
      action="<%=PageBackendService.encodeURL( response, ViewManager.htmlView + "/" + qzRequest.getManageType() + "/" + qzRequest.getAppName() + "/" + qzRequest.getModelName() + "/" + ListModel.ACTION_NAME_LIST)%>">
    <div class="row filterForm" style="margin-top: 10px;">
        <%
            int index = 1;
            for (Integer i : indexToShow) {
                String fieldName = modelManager.getFieldName(qzRequest.getModelName(), i);
                List<Option> modelOptionsEntry = null;
                if (PageBackendService.isFilterSelect(qzRequest, i)) {
                    try {
                        Options modelOptions = modelManager.getOptions(qzRequest.getModelName(), fieldName);
                        if (modelOptions != null) {
                            modelOptionsEntry = modelOptions.options();
                        }
                    } catch (Exception ignored) {
                    }
                }
                boolean changeLast = (index++ == indexToShow.size() && indexToShow.size() % 4 == 0);
        %>
        <div class='<%=changeLast?"col-md-2":"col-md-3"%> list-page-padding-bottom <%=modelOptionsEntry != null ? "listPageFilterSelect" : "" %>'>
            <div class="input-control">
                <%
                    if (modelOptionsEntry != null) {
                %>
                <%@ include file="../fragment/filter_select.jsp" %>
                <%
                } else {
                    String showHtml = (request.getParameter(fieldName) == null) ? "" : request.getParameter(fieldName);
                    if (StringUtil.notBlank(showHtml)) {
                        if (SafeCheckerUtil.checkIsXSS(showHtml)) {
                            showHtml = "";
                        }
                    }
                %>
                <input id="<%=fieldName%>" type="text" name="<%=fieldName%>" value='<%=showHtml%>' class="form-control"
                       placeholder="<%=I18n.getString(menuAppName, "model.field." + qzRequest.getModelName() + "." + fieldName)%>">
                <%
                    }
                %>
            </div>
        </div>
        <%
            }
        %>
        <div class="col-md-1 search-btn" style="margin-bottom: 16px;">
                        <span class="input-group-btn col-md-4" style="width: 18%;padding-left:0px;">
                            <a class="btn"
                               href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest,ViewManager.htmlView,ListModel.ACTION_NAME_LIST)%>"
                               form="filterForm">
                                <i class="icon icon-search"></i> <%=PageBackendService.getMasterAppI18NString("page.filter")%>
                            </a>
                        </span>
        </div>
    </div>
</form>