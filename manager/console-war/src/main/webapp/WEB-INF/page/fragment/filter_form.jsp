<%@ page import="java.util.stream.Collectors" %>
<%@ page pageEncoding="UTF-8" %>

<form name="filterForm" id="filterForm" method="POST"
      action="<%=PageBackendService.encodeURL( response, ViewManager.htmlView + "/" + qzRequest.getManageType() + "/" + qzRequest.getApp() + "/" + qzRequest.getModel() + "/" + Listable.ACTION_NAME_LIST)%>">
    <div class="row filterForm" style="margin-top: 10px; display: none;">
        <%
            for (Integer i : indexToShow) {
                String fieldName = PageBackendService.getFieldName(qzRequest, i);
                List<String> modelOptionsEntry = null;
                if (PageBackendService.isFilterSelect(qzRequest, i)) {
                    try {
                        String[] modelOptions = modelInfo.getFieldOptions(fieldName);
                        if (modelOptions != null) {
                            modelOptionsEntry = Arrays.stream(modelOptions).collect(Collectors.toList());
                        }
                    } catch (Exception ignored) {
                    }
                }
        %>
        <div class='col-md-2 col-sm-3 col-xs-4 list-page-padding-bottom'>
            <div class="input-control">
                <%
                    if (modelOptionsEntry != null) {
                %>
                <%@ include file="../fragment/filter_select.jsp" %>
                <%
                } else {
                    String showHtml = request.getParameter(fieldName);
                    if (showHtml == null) {
                        showHtml = "";
                    }
                %>
                <input id="<%=fieldName%>" type="text" name="<%=fieldName%>" value='<%=showHtml%>' class="form-control"
                       placeholder="<%=I18n.getString(menuAppName, "model.field." + qzRequest.getModel() + "." + fieldName)%>">
                <%
                    }
                %>
            </div>
        </div>
        <%
            }
        %>
        <div class="col-md-2 col-sm-3 col-xs-4 search-btn" style="margin-bottom: 16px;">
                        <span class="input-group-btn col-md-4" style="width: 18%;padding-left:0px;">
                            <a class="btn"
                               href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest,ViewManager.htmlView,Listable.ACTION_NAME_LIST)%>"
                               form="filterForm">
                                <i class="icon icon-search"></i> <%=PageBackendService.getMasterAppI18nString("page.filter")%>
                            </a>
                        </span>
        </div>
    </div>
</form>