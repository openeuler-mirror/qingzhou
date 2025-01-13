<%@ page pageEncoding="UTF-8" %>

<%@ include file="../fragment/head.jsp" %>

<%
	String contextPath = request.getContextPath();
	String idField = modelInfo.getIdField();
	ModelFieldInfo idFieldFieldInfo = modelInfo.getModelFieldInfo(idField);

	String[] dataListFields = modelInfo.getFieldsToList();
	String[] displayListFields = modelInfo.getFieldsToList();

	if (!idFieldFieldInfo.isShow()) {
		List<String> temp = new ArrayList<>(Arrays.asList(displayListFields));
		temp.remove(idField);
		displayListFields = temp.toArray(new String[0]);
	}

	String[] listActions = PageUtil.filterActions(modelInfo.getListActions(), qzApp, qzModel, request);
	String[] headActions = PageUtil.filterActions(modelInfo.getHeadActions(), qzApp, qzModel, request);
	String[] batchActions = PageUtil.filterActions(modelInfo.getBatchActions(), qzApp, qzModel, request);

	ListData listData = (ListData) qzResponse.getInternalData();
	if (listData == null) return;
	int totalSize = listData.totalSize;
	int pageNum = listData.pageNum;
	int pageSize = listData.pageSize;

	boolean isEdit = false; // for field_type.jsp
	boolean isDisabled = false; // for field_type.jsp
%>

<div class="bodyDiv">
	<%-- 面包屑分级导航 --%>
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<%
		String[] fieldsToListSearch = modelInfo.getFieldsToListSearch();
		if (fieldsToListSearch.length > 0) {
	%>
	<%@ include file="../fragment/filter_form.jsp" %>
	<hr style="margin-top: 4px;">
	<%
		}

		Set<String> subFormActions = new LinkedHashSet<>(Arrays.asList(headActions));
		subFormActions.addAll(Arrays.asList(listActions));
		for (String actionName : subFormActions) {
			ModelActionInfo action = modelInfo.getModelActionInfo(actionName);
			if (action.getActionType() == ActionType.sub_form) {
				Map<String, String> actionFormData = new LinkedHashMap<>();
				for (String fieldName : action.getSubFormFields()) {
					if (modelInfo.getModelFieldInfo(fieldName) != null) {
						actionFormData.put(fieldName, "");
					}
				}
				String popupActionId = " sub_form_action_id='" + qzApp + "-" + qzModel + "-" + actionName + "'"
						+ " sub_form_base_url=" + PageUtil.buildModelUrl(request, response, JsonView.FLAG, qzApp, qzModel)
						+ " sub_form_action_name=" + actionName
						+ " sub_form_action_title=" + I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionName)
						+ " sub_form_autoload=" + action.isSubFormAutoload()
						+ " sub_form_autoclose=" + action.isSubFormAutoclose();
	%>
	<div style="display: none" <%=popupActionId%>>
		<%@ include file="../fragment/action_form.jsp" %>
	</div>
	<%
			}
		}
	%>

	<div class="table-tools qz-list-operate">
		<div class="tools-group">
			<%
				for (String actionName : headActions) {
					ModelActionInfo action = modelInfo.getModelActionInfo(actionName);
					String viewName = qzRequest.getView();

					if (action.getActionType() == ActionType.upload) {
						viewName = JsonView.FLAG;
			%>
			<a href="javascript:;"
			   onclick="$('#<%=actionName%>').click();" class="btn uploader-btn-browse">
				<i class="icon icon-<%=action.getIcon()%>"></i>
				<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionName)%>
			</a>
			<input id="<%=actionName%>" type="file" style="display:none;"
				   onchange="upload(this.files[0],
						   '<%= PageUtil.buildRequestUrl(request, response, qzRequest, viewName, actionName) %>','<%=actionName%>')">
			<%
					continue;
				}

				if (action.getActionType() == ActionType.download || action.getActionType() == ActionType.export) {
					viewName = DownloadView.FLAG;
				}

				String dataTip = I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionName);
				if (Utils.isBlank(dataTip)) {
					dataTip = I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionName);
				}
			%>
			<a class="btn" data-tip-arrow="top" action-id="<%=qzApp + "-" + qzModel + "-" + actionName%>"
			   data-tip='<%=dataTip%>'
			   action-type="<%=action.getActionType()%>"
			   href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, viewName, actionName)%>"
			>
				<i class="icon icon-<%=action.getIcon()%>"></i>
				<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionName)%>
			</a>
			<%
				}

				String randomId = UUID.randomUUID().toString();
				// 支持批量操作的按钮
				for (String actionKey : batchActions) {
					ModelActionInfo actionInfo = modelInfo.getModelActionInfo(actionKey);

					String operationConfirm = String.format(I18n.getKeyI18n("page.operationConfirm"),
							I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey),
							I18n.getModelI18n(qzApp, "model." + qzModel));

					String actionUrl = PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG, actionKey);
			%>
			<a href="<%=actionUrl%>" model-icon="<%=modelInfo.getIcon()%>" disabled="disabled"
			   action-type="<%=actionInfo.getActionType()%>"
			   id-name="<%=idField%>" id-separa="<%=idFieldFieldInfo.getSeparator()%>"
			   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionKey)%>'
			   class="btn batch-ops" act-confirm='<%=operationConfirm%> ?' binding="<%=randomId%>">
				<i class="icon icon-<%=actionInfo.getIcon()%>"></i>
				<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey)%>
			</a>
			<%
				}
			%>

			<%
				if (request.getAttribute(DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID) != null && !"".equals(request.getAttribute(DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID))) {
					out.print("<a href='javascript:void(0);' class='btn' linkId='" + request.getAttribute(DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID) + "' onclick='backToList(this);'><i class='icon icon-reply'></i>" + I18n.getKeyI18n("page.return") + "</a>");
				}
			%>
		</div>
	</div>

	<table class="qz-data-list table table-striped table-hover list-table responseScroll" binding="<%=randomId%>">
		<thead>
		<tr style="height:20px;">
			<%
				int otherTh = 0;
				if (batchActions.length > 0) {
					otherTh += 1;
			%>
			<th class="custom-checkbox" style="width: 2%">
				<input type="checkbox" class="allcheck"/>
			</th>
			<%
				}
				if (modelInfo.isShowOrderNumber()) {
					otherTh += 1;
			%>
			<th class="sequence" style="width: 2%"><%=I18n.getKeyI18n("page.list.order")%>
			</th>
			<%
				}
				if (listActions.length > 0) {
					otherTh += 1;
				}
				//隐藏的不计算宽度占比
				int hiddenCount = 0;
				for (String field : displayListFields) {
					ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
					if (fieldInfo.isHidden()) {
						hiddenCount += 1;
					}
				}
				for (String field : displayListFields) {
					ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
					String needHidden = "";
					if (fieldInfo.isHidden()) {
						needHidden = "display:none";
					}
					int width;
					if (fieldInfo.getWidthPercent() > 0) {
						width = fieldInfo.getWidthPercent();
					} else {
						// 多选框占 2%，序号占 2%，剩余 96% ping j
						width = 96 / (displayListFields.length + otherTh - hiddenCount);
					}
			%>
			<%-- 注意这个width末尾的 % 不能删除 %>% 不是手误 --%>
			<th style="width: <%=width%>%;<%=needHidden%>"><%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + field)%>
			</th>
			<%
				}
				if (listActions.length > 0) {
					out.print("<th style=\"width:" + (100 / (displayListFields.length + otherTh - hiddenCount)) + "%\">" + I18n.getKeyI18n("page.action") + "</th>");
				}
			%>
		</tr>
		</thead>
		<tbody>
		<%
			List<String[]> modelDataList = listData.dataList;
			if (modelDataList.isEmpty()) {
				String dataEmpty = "<tr><td colspan='" + (((batchActions.length > 0) ? 1 : 0) + (modelInfo.isShowOrderNumber() ? 1 : 0) + displayListFields.length + (listActions.length > 0 ? 1 : 0)) + "' align='center'>"
						+ "<img src='" + contextPath + "/static/images/data-empty.svg' style='width:160px; height: 160px;'><br>"
						+ "<span style='font-size:14px; font-weight:600; letter-spacing: 2px;'>" + I18n.getKeyI18n("page.none") + "</span></td>";
				out.print(dataEmpty);
			} else {
				int listOrder = (pageNum - 1) * pageSize;
				for (String[] modelData : modelDataList) {
					String originUnEncodedId = modelData[PageUtil.getIndex(dataListFields, idField)];
					String encodedItemId = RESTController.encodeId(originUnEncodedId);
		%>
		<tr>
			<%
				if (batchActions.length > 0) {
			%>
			<td class="custom-checkbox">
				<input type="checkbox" class='morecheck' value="<%=encodedItemId%>"/>
			</td>
			<%
				}
				if (modelInfo.isShowOrderNumber()) {
			%>
			<td class="sequence"><%=++listOrder%>
			</td>
			<%
				}
				for (String field : displayListFields) {
					ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);

					String needHidden = "";
					if (fieldInfo.isHidden()) {
						needHidden = "display:none";
					}

					String value = modelData[PageUtil.getIndex(dataListFields, field)];
					if (value == null) {
						value = "";
					}

					String fieldUpdateAction = null;
					if (Utils.notBlank(fieldInfo.getUpdateAction())
							&& SecurityController.isActionPermitted(qzApp, qzModel, fieldInfo.getUpdateAction(), request, dataListFields, modelData)) {
						fieldUpdateAction = fieldInfo.getUpdateAction();
					}
					if (fieldUpdateAction != null) {

			%>
			<td style="<%=needHidden%>"
				action="<%=PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG , fieldUpdateAction + "/" + encodedItemId)%>">
					<%
            } else {
            %>
			<td style="<%=needHidden%>"><%
				}

				// 以下是 当前 <td> 的内容
				if (fieldUpdateAction != null) {
					// 兼容表单组件 例：bool.jsp中使用的是fieldValue和fieldName
					// 避免编译报错，放入这个循环里，是避免和 list.jsp 中 的同名变量冲突
					List<String> passwordFields = new ArrayList<>();
					String echoGroup = "";
					String fieldValue = value;
					List<String> fieldValues = Arrays.asList(fieldValue.split(fieldInfo.getSeparator()));
					String fieldName = field;
					ModelFieldInfo modelField = fieldInfo;

					if (fieldInfo.getInputType() == InputType.bool) {
			%>
				<%@ include file="../fragment/field_type/bool.jsp" %>
				<%
				} else if (fieldInfo.getInputType().equals(InputType.grouped_multiselect)) {
				%>
				<%@ include file="../fragment/field_type/grouped_multiselect.jsp" %>
				<%
				} else if (ValidationFilter.isSingleSelect(fieldInfo)) {
				%>
				<%@ include file="../fragment/field_type/select.jsp" %>
				<%
				} else if (ValidationFilter.isMultipleSelect(fieldInfo)) {
				%>
				<%@ include file="../fragment/field_type/multiselect.jsp" %>
				<%
					} else {
						out.print("<div class=\"input-class\"> <input type=\"text\" name=\"" + fieldName + "\" value=\"" + fieldValue + "\" class=\"form-control\"></div>");
					}
				} else if (Utils.notBlank(fieldInfo.getLinkAction())
						&& SecurityController.isActionPermitted(qzApp, qzModel, fieldInfo.getLinkAction(), request, dataListFields, modelData)) {
					ModelActionInfo linkActionInfo = modelInfo.getModelActionInfo(fieldInfo.getLinkAction());
				%>
				<a href='<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG , fieldInfo.getLinkAction() + "/" + encodedItemId)%>'
				   class="dataid qz-action-link tooltips"
				   action-type="<%=fieldInfo.getActionType()%>"
				   data-tip-arrow="top"
				   data-tip='<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + linkActionInfo.getCode())%>'
				   style="color:#4C638F;">
					<%=PageUtil.styleFieldValue(value, qzRequest, fieldInfo)%>
				</a>
				<%
				} else {
					String refModelName = null;
					String refFieldName = null;
					String refValue = null;
					if (Utils.notBlank(fieldInfo.getLinkModel())) {
						//currentModelFieldName>targetModelName
						String[] split = fieldInfo.getLinkModel().split(">");
						refFieldName = split[0];
						refModelName = split[1];
						//linkList传表达式指定的值
						if (refFieldName.contains(",")) {
							List<String> refValueArr = new LinkedList<>();
							for (String name : refFieldName.split(",")) {
								refValueArr.add(modelData[PageUtil.getIndex(dataListFields, name)]);
							}
							refValue = String.join("##", refValueArr);
						} else {
							refValue = modelData[PageUtil.getIndex(dataListFields, refFieldName)];
						}
					} else if (Utils.notBlank(fieldInfo.getRefModel())) {
						refModelName = fieldInfo.getRefModel();
						refFieldName = SystemController.getModelInfo(qzApp, refModelName).getIdField();
						if (Utils.notBlank(value)) {
							//应用跳转到实例：refmodel 传点击字段的值
							ModelFieldInfo refFieldInfo = SystemController.getModelInfo(qzApp, refModelName).getModelFieldInfo(refFieldName);
							refValue = value.replace(fieldInfo.getSeparator(), refFieldInfo.getSeparator());
						}
					}

					ModelActionInfo linkRefModelActionInfo = null;
					if (refModelName != null && refFieldName != null && refValue != null) {
						if (SecurityController.isActionPermitted(qzApp, refModelName, qingzhou.api.type.List.ACTION_LIST, request, dataListFields, modelData)) {
							linkRefModelActionInfo = SystemController.getModelInfo(qzApp, refModelName).getModelActionInfo(qingzhou.api.type.List.ACTION_LIST);
						}
					}
					if (linkRefModelActionInfo != null) {
						StringBuilder urlParams = new StringBuilder();
						if (refFieldName.contains(",")) {
							String[] refFieldNameArr = refFieldName.split(",");
							String[] refValueArr = refValue.split("##");
							for (int i = 0; i < refFieldNameArr.length; i++) {
								urlParams.append(refFieldNameArr[i])
										.append("=")
										.append(URLEncoder.encode(refValueArr[i], "UTF-8"))
										.append("&");
							}
							urlParams.append("refFieldName")
									.append("=")
									.append(refFieldName)
									.append("&");
						} else {
							urlParams.append(refFieldName)
									.append("=")
									.append(URLEncoder.encode(refValue, "UTF-8"))
									.append("&");
						}
				%>
				<a href='<%=PageUtil.buildCustomUrl(request, response, qzRequest,HtmlView.FLAG, refModelName, qingzhou.api.type.List.ACTION_LIST + "?" + urlParams)%>'
				   class="dataid qz-action-link tooltips"
				   action-type="<%=fieldInfo.getActionType()%>"
				   data-tip='<%=I18n.getModelI18n(qzApp, "model." + refModelName)%>' data-tip-arrow="top"
				   style="color:#4C638F;" onclick='actionModelMenu("<%=qzRequest.getModel()%>","<%=refModelName%>")'>
					<%=PageUtil.styleFieldValue(value, qzRequest, fieldInfo)%>
				</a>
				<%
				} else if (field.equals(idField)
						&& SecurityController.isActionPermitted(qzApp, qzModel, Show.ACTION_SHOW, request, dataListFields, modelData)) {
					ModelActionInfo showActionInfo = modelInfo.getModelActionInfo(Show.ACTION_SHOW);
					String condition = showActionInfo.getDisplay();
					if (Utils.notBlank(condition)) {
						if (!SecurityController.checkRule(condition, fieldName -> modelData[PageUtil.getIndex(dataListFields, fieldName)])) {
							showActionInfo = null;
						}
					}
					if (showActionInfo != null) {
				%>
				<a href='<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG , Show.ACTION_SHOW + "/" + encodedItemId)%>'
				   class="dataid qz-action-link tooltips"
				   action-type="<%=showActionInfo.getActionType()%>"
				   data-tip-arrow="top"
				   data-tip='<%=I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + field)%>'
				   style="color:#4C638F;">
					<%=PageUtil.styleFieldValue(value, qzRequest, fieldInfo)%>
				</a>
				<%
							} else {
								out.print(PageUtil.styleFieldValue(value, qzRequest, fieldInfo));
							}
						} else {
							out.print(PageUtil.styleFieldValue(value, qzRequest, fieldInfo));
						}
					}

					// 以上是 当前 <td> 的内容
				%>
			</td>
			<%
				}

				if (listActions.length > 0) {
			%>
			<td>
				<%
					for (String actionName : listActions) {
						ModelActionInfo action = modelInfo.getModelActionInfo(actionName);
						boolean showAction = true;
						if (Utils.notBlank(action.getDisplay())) {
							showAction = SecurityController.checkRule(action.getDisplay(), fieldName -> modelData[PageUtil.getIndex(dataListFields, fieldName)]);
						}
						if (!showAction) continue;

						boolean useJsonUri = action.getActionType() == ActionType.action_list
								|| action.getActionType() == ActionType.download;
				%>
				<a style="white-space: nowrap;"
				   href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, useJsonUri ? JsonView.FLAG : HtmlView.FLAG, actionName + "/" + encodedItemId)%>"
				   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionName)%>'
				   data-tip-arrow="top" action-id="<%=qzApp + "-" + qzModel + "-" + actionName%>"
				   class="qz-action-link tooltips" model-icon="<%=modelInfo.getIcon()%>"
				   data-id="<%=(qzModel + "|" + encodedItemId)%>" action-type="<%=action.getActionType()%>"
				   data-name="<%=originUnEncodedId%>"
						<%
							if (action.getActionType() == ActionType.download) {
								out.print(" downloadfile='" + PageUtil.buildRequestUrl(request, response, qzRequest, DownloadView.FLAG, Download.ACTION_DOWNLOAD + "/" + encodedItemId) + "'");
							} else if (action.getActionType() == ActionType.sub_form) {
								out.print(" sub_form_id='" + encodedItemId + "'");
							} else if (action.getActionType() == ActionType.action_list) {
								out.print(" act-confirm='"
										+ String.format(I18n.getKeyI18n("page.operationConfirm"),
										I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionName),
										I18n.getModelI18n(qzApp, "model." + qzModel))
										+ " " + originUnEncodedId + " ?"
										+ "'");
							}
						%>
				>
					<i class="icon icon-<%=action.getIcon()%>"></i>
					<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionName)%>
				</a>
				<%
					}
				%>
			</td>
			<%
				}
			%>
		</tr>
		<%
				}
			}
		%>
		</tbody>
	</table>

	<%
		if (SecurityController.isActionPermitted(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, request)) {
			String url = PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, qingzhou.api.type.List.ACTION_LIST);
			url += (url.contains("?") ? "&" : "?") + "markForAddCsrf";
			if (request.getAttribute(DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID) != null && !url.contains("&" + DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID)) {
				url += "&" + DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID + "=" + request.getAttribute(DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID);
			}
			//追加link_model的参数
			String refFieldName = request.getParameter("refFieldName");
			if (refFieldName != null && !refFieldName.isEmpty()) {
				if (refFieldName.contains(",")) {
					String[] refFieldNameArr = refFieldName.split(",");
					for (String refName : refFieldNameArr) {
						url += "&" + refName + "=" + request.getParameter(refName).replaceAll("\"", "&quot;");
					}
				} else {
					url += "&" + refFieldName + "=" + request.getParameter(refFieldName).replaceAll("\"", "&quot;");
				}
				url += "&refFieldName=" + refFieldName;
			}
			url += "&" + ListData.PAGE_NUM + "=";
	%>
	<div style="text-align: center; <%=(totalSize <= 0) ? "display:none;" : ""%>">
		<ul class="pager pager-loose" data-ride="pager" data-page="<%=pageNum%>"
			recPerPage="<%=pageSize%>"
			data-rec-total="<%=totalSize%>"
			partLinkUri="<%=url%>"
			style="margin-left:33%;color:black;margin-bottom:6px;">
		</ul>
	</div>
	<%
		}
	%>
</div>

<script>
    $(".allcheck", getRestrictedArea()).click(function () {
        if (!$(this).prop("checked")) {
            $(this).prop("checked", false);
            $(".list-table td input[type='checkbox'][class='morecheck']", getRestrictedArea()).prop("checked", false);
            $(".batch-ops", getRestrictedArea()).attr("disabled", "disabled");
        } else {
            $(this).prop("checked", "checked");
            $(".list-table td input[type='checkbox'][class='morecheck']", getRestrictedArea()).prop("checked", "checked");
            $(".batch-ops", getRestrictedArea()).removeAttr("disabled");
        }
    });

    $(".morecheck", getRestrictedArea()).click(function () {
        if (!$(this).prop("checked")) {
            $(this).removeAttr("checked", false);
            $(".allcheck", getRestrictedArea()).prop("checked", false);
        } else {
            $(this).prop("checked", "checked");
            var isAll = true;
            var morechecks = $(".morecheck", getRestrictedArea());
            for (var i = 0; i < morechecks.length; i++) {
                if (!morechecks[i].checked) {
                    isAll = false;
                    break;
                }
            }
            if (isAll) {
                $(".allcheck", getRestrictedArea()).prop("checked", "checked");
            }
        }
        if ($('.morecheck:checkbox:checked', getRestrictedArea()).length > 0) {
            $(".batch-ops", getRestrictedArea()).removeAttr("disabled");
        } else {
            $(".batch-ops", getRestrictedArea()).attr("disabled", "disabled");
        }
    });

    $(document).ready(function () {
        $('[data-toggle="tooltip"]').tooltip();
    });
</script>
