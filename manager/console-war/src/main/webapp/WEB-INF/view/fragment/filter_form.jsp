<%@ page pageEncoding="UTF-8" %>

<%
    if (fieldsToListSearch.length > 0) {
%>

<form name="filterForm" class="filterForm" method="POST"
      action="<%=RESTController.encodeURL( response, HtmlView.FLAG + "/" + qzApp + "/" + qzModel + "/" + qzAction + (Utils.notBlank(encodedId) ? "/" + encodedId : ""))%>">
    <div class="row" style="margin-top: 10px;">
        <%
            for (String fieldName : fieldsToListSearch) {
                ModelFieldInfo searchFieldInfo = modelInfo.getModelFieldInfo(fieldName);
                String colClass = "col-md-2 col-sm-3 col-xs-4";
                String dateTimeStyle = "";
                if (searchFieldInfo.getInputType() == InputType.textarea) {
                    colClass = "col-md-12 col-sm-12 col-xs-12";
                } else if (searchFieldInfo.getInputType() == InputType.datetime) {
                    colClass = "col-md-4 col-sm-6 col-xs-8";
                    dateTimeStyle = "display:flex";
                }
        %>
        <div class='<%=colClass%> list-page-padding-bottom'>
            <div class="input-control" style="<%=dateTimeStyle%>">
                <%
                    String echoGroup = "";
                    String fieldValue = null;
                    if (ValidationFilter.filterPageIsMultipleSelect(searchFieldInfo)
                            || searchFieldInfo.getInputType() == InputType.datetime) {
                        if (request.getParameterValues(fieldName) != null) {
                            fieldValue = String.join(searchFieldInfo.getSeparator(), request.getParameterValues(fieldName));
                        }
                    } else {
                        fieldValue = request.getParameter(fieldName);
                    }
                    if (fieldValue == null) {
                        Map<String, String> defaultSearch = modelInfo.getDefaultSearch();
                        if (defaultSearch != null) {
                            fieldValue = defaultSearch.get(fieldName);
                        }
                    }
                    if (fieldValue == null) {
                        fieldValue = "";
                    }
                    if (ValidationFilter.filterPageIsMultipleSelect(searchFieldInfo)) {
                        java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(searchFieldInfo.getSeparator()));
                %>
                <%@ include file="field_type/multiselect.jsp" %>
                <%
                } else if (searchFieldInfo.getInputType() == InputType.textarea) {
                %>
                <textarea name="<%=fieldName%>" value='<%=fieldValue%>' class="form-control"
                          rows="2"
                          placeholder="<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>"><%=fieldValue%></textarea>
                <%
                } else if (searchFieldInfo.getInputType() == InputType.datetime) {
                    java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(searchFieldInfo.getSeparator()));
                %>
                <%@ include file="field_type/rangedatetime.jsp" %>
                <%
                } else {
                %>
                <input id="<%=fieldName%>" type="text" name="<%=fieldName%>" value='<%=fieldValue%>'
                       class="form-control"
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
                <a class="btn filter_search"
                   href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, qzAction + (Utils.notBlank(encodedId) ? "/" + encodedId : ""))%>">
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
