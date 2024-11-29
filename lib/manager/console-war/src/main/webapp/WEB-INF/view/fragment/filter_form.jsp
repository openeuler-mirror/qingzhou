<%@ page pageEncoding="UTF-8" %>

<%
	if (fieldsToListSearch.length > 0) {
%>

<form name="filterForm" class="filterForm" method="POST"
	  action="<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, qzAction + (Utils.notBlank(encodedId) ? "/" + encodedId : ""))%>">
	<div class="row" style="margin-top: 10px;">
		<%
			for (String fieldName : fieldsToListSearch) {
				ModelFieldInfo searchFieldInfo = modelInfo.getModelFieldInfo(fieldName);
				ModelFieldInfo modelField = searchFieldInfo; // range_datetime.jsp 使用
				String inputType = "";
				if (ValidationFilter.isMultipleSelect(searchFieldInfo) || (ValidationFilter.isSingleSelect(searchFieldInfo) && searchFieldInfo.isMultipleSearch())){
					inputType = "multiselect";
				}else if(ValidationFilter.isSingleSelect(searchFieldInfo)){
					inputType = "select";
				}else if(searchFieldInfo.getInputType().equals(InputType.grouped_multiselect)){
					inputType = "grouped_multiselect";
				}

				String colClass = "col-md-2 col-sm-3 col-xs-4";
				if (searchFieldInfo.getInputType() == InputType.textarea) {
					colClass = "col-md-12 col-sm-12 col-xs-12";
				} else if (searchFieldInfo.getInputType() == InputType.range_datetime) {
					colClass = "col-md-3 col-sm-5 col-xs-7";
				} else if (searchFieldInfo.getInputType() == InputType.combine) {
					colClass = "col-md-4 col-sm-6 col-xs-12";
				}
		%>
		<div class='<%=colClass%> list-page-padding-bottom'>
			<div class="input-control" id="form-item-<%=fieldName%>" type="<%=inputType%>">
				<%
					String echoGroup = "";
					String fieldValue = qzRequest.getParameter(fieldName);
					if (fieldValue == null) {
						Map<String, String> defaultSearch;
						if (modelInfo.isUseDynamicDefaultSearch()) {
							RequestImpl tmp = new RequestImpl(qzRequest);
							tmp.setActionName(qingzhou.api.type.List.ACTION_DEFAULT_SEARCH);
							ResponseImpl tmpResp = (ResponseImpl) SystemController.getService(ActionInvoker.class).invokeSingle(tmp);
							defaultSearch = (Map<String, String>) tmpResp.getInternalData();
						} else {
							defaultSearch = modelInfo.getDefaultSearch();
						}
						if (defaultSearch != null) {
							fieldValue = defaultSearch.get(fieldName);
						}
					}
					if (fieldValue == null) {
						fieldValue = "";
					}
					if (modelField.getEchoGroup().length > 0) {
						String echoGroups = String.join(",", modelField.getEchoGroup());
						echoGroup = "echoGroup=" + echoGroups ;
					}
					if (ValidationFilter.isMultipleSelect(searchFieldInfo) || (ValidationFilter.isSingleSelect(searchFieldInfo) && searchFieldInfo.isMultipleSearch())) {
						java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(searchFieldInfo.getSeparator()));
				%>
				<%@ include file="field_type/multiselect.jsp" %>
				<%
				} else if (ValidationFilter.isSingleSelect(searchFieldInfo)) {
				%>
				<%@ include file="field_type/select.jsp" %>
				<%
				} else if (searchFieldInfo.getInputType().equals(InputType.grouped_multiselect)) {
					java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(searchFieldInfo.getSeparator()));
				%>
				<%@ include file="field_type/grouped_multiselect.jsp" %>
				<%
				} else if (searchFieldInfo.getInputType() == InputType.textarea) {
				%>
				<textarea name="<%=fieldName%>" value='<%=fieldValue%>' class="form-control"
						  rows="2"
						  placeholder="<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>"><%=fieldValue%></textarea>
				<%
				} else if (searchFieldInfo.getInputType() == InputType.range_datetime) {
				%>
				<%@ include file="field_type/range_datetime.jsp" %>
				<%
				} else if (searchFieldInfo.getInputType() == InputType.combine){
				%>
				<%@ include file="field_type/combine.jsp" %>
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
                <a class="btn" href="javascript:void(0);"
				   onclick="(function(element) {$('.treeview.active:last > a', $(element).closest('section.main-body').prev()).trigger('click')})(this);">
                    <i class="icon icon-search"></i> <%=I18n.getKeyI18n("page.filter.reset")%>
                </a>
            </span>
		</div>
	</div>
</form>

<%
	}
%>
