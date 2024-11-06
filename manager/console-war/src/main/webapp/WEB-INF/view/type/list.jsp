<%@ page pageEncoding="UTF-8" %>

<%@ include file="../fragment/head.jsp" %>

<%
    String contextPath = request.getContextPath();
    String idField = modelInfo.getIdField();
    ModelFieldInfo idFieldFieldInfo = modelInfo.getModelFieldInfo(idField);

    String[] listFields = modelInfo.getFieldsToList();

    if (modelInfo.isHideId()) {
        List<String> temp = new ArrayList<>(Arrays.asList(listFields));
        temp.remove(idField);
        listFields = temp.toArray(new String[0]);
    }

    String[] listActions = PageUtil.filterActions(modelInfo.getListActions(), qzApp, qzModel, currentUser);
    String[] headActions = PageUtil.filterActions(modelInfo.getHeadActions(), qzApp, qzModel, currentUser);
    String[] batchActions = PageUtil.filterActions(modelInfo.getBatchActions(), qzApp, qzModel, currentUser);

    int totalSize = qzResponse.getTotalSize();
    int pageNum = qzResponse.getPageNum();
    int pageSize = qzResponse.getPageSize();
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

                if (action.getActionType() == ActionType.download) {
                    viewName = DownloadView.FLAG;
                }

                String customActionId = "";
                if (action.getActionType() == ActionType.sub_form) {
                    viewName = JsonView.FLAG;
                    customActionId = " custom-action-id='popup-" + qzApp + "-" + qzModel + "-" + action.getCode() + "'";
                }
            %>
            <a class="btn" data-tip-arrow="top" <%=customActionId%> action-type="<%=action.getActionType()%>"
               data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionName)%>'
               href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, viewName, actionName)%>"
            >
                <i class="icon icon-<%=action.getIcon()%>"></i>
                <%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionName)%>
            </a>
            <%
                if (action.getActionType() == ActionType.sub_form) {
                    Map<String, String> actionFormData = new LinkedHashMap<>();
                    for (String fieldName : modelInfo.getFormFieldNames()) {
                        if (Utils.contains(action.getFormFields(), fieldName)) {
                            actionFormData.put(fieldName, "");
                        }
                    }
            %>
            <div style="display: none" <%=customActionId%>>
                <%@ include file="../fragment/action_form.jsp" %>
            </div>
            <%
                    }
                }

                String randomId = java.util.UUID.randomUUID().toString();
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
			<th class="custom-checkbox">
				<input type="checkbox" class="allcheck"/>
			</th>
			<%
				}
				if (modelInfo.isShowOrderNumber()) {
					otherTh += 1;
			%>
			<th class="sequence"><%=I18n.getKeyI18n("page.list.order")%>
			</th>
			<%
				}
				if (listActions.length > 0) {
					otherTh += 1;
				}
                //隐藏的不计算宽度占比
                int hiddenCount = 0;
                for (String field : listFields) {
                    ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
                    if (fieldInfo.isHidden()) {
                        hiddenCount += 1;
                    }
                }
				for (String field : listFields) {
					ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
					String needHidden = "";
					if (fieldInfo.isHidden()){
						needHidden = "display:none";
					}
					int width;
					if (fieldInfo.getWidthPercent() > 0) {
						width = fieldInfo.getWidthPercent();
					} else {
						width = 100 / (listFields.length + otherTh - hiddenCount);
					}
			%>
			<%-- 注意这个width末尾的 % 不能删除 %>% 不是手误 --%>
			<th style="width: <%=width%>%;<%=needHidden%>"><%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + field)%>
			</th>
			<%
				}
				if (listActions.length > 0) {
					out.print("<th style=\"width:"+ (100 / (listFields.length + otherTh - hiddenCount)) +"%\">" + I18n.getKeyI18n("page.action") + "</th>");
				}
			%>
		</tr>
		</thead>
		<tbody>
		<%
			java.util.List<String[]> modelDataList = qzResponse.getDataList();
			if (modelDataList.isEmpty()) {
				String dataEmpty = "<tr><td colspan='" + (((batchActions.length > 0) ? 1 : 0) + (modelInfo.isShowOrderNumber() ? 1 : 0) + listFields.length + (listActions.length > 0 ? 1 : 0)) + "' align='center'>"
						+ "<img src='" + contextPath + "/static/images/data-empty.svg' style='width:160px; height: 160px;'><br>"
						+ "<span style='font-size:14px; font-weight:600; letter-spacing: 2px;'>" + I18n.getKeyI18n("page.none") + "</span></td>";
				out.print(dataEmpty);
			} else {
				int listOrder = (pageNum - 1) * pageSize;
				int idIndex = modelInfo.getIdIndex();
				for (String[] modelData : modelDataList) {
					String originUnEncodedId = modelData[idIndex];
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
				for (String field : listFields) {
					ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
					String needHidden = "";
					if (fieldInfo.isHidden()){
						needHidden = "display:none";
					}
					String value = modelData[modelInfo.getFieldIndex(field)];
					if (value == null) {
						value = "";
					}
					String fieldUpdateAction = "";
					if (!fieldInfo.getUpdateAction().isEmpty()) {
						fieldUpdateAction = fieldInfo.getUpdateAction();
			%>
			<td style="<%=needHidden%>" action="<%=PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG , fieldUpdateAction + "/" + encodedItemId)%>">
					<%
                    }else{
            %>
			<td style="<%=needHidden%>">
				<%
					}
				%>
				<%
					if ((field.equals(idField)) || fieldInfo.isLinkShow()) {
						boolean hasShowAction = false;
						if (SecurityController.isActionPermitted(qzApp, qzModel, Show.ACTION_SHOW, currentUser)) {
							hasShowAction = true;
							String condition = modelInfo.getModelActionInfo(Show.ACTION_SHOW).getShow();
							if (Utils.notBlank(condition)) {
								hasShowAction = SecurityController.checkRule(condition, fieldName -> modelData[modelInfo.getFieldIndex(fieldName)]);
							}
						}
						if (hasShowAction) {
				%>

                <a href='<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG , Show.ACTION_SHOW + "/" + encodedItemId)%>'
                   class="dataid qz-action-link tooltips"
                   data-tip-arrow="top"
                   data-tip='<%=I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + field)%>'
                   style="color:#4C638F;">
                    <%=PageUtil.styleFieldValue(value, fieldInfo, modelInfo)%>
                </a>
                <%
                    } else {
                        out.print(PageUtil.styleFieldValue(value, fieldInfo, modelInfo));
                    }
                } else if (!fieldInfo.getUpdateAction().isEmpty()) {
                    // 兼容表单组件 例：bool.jsp中使用的是fieldValue和fieldName
                    // 避免编译报错，放入这个循环里，是避免和 list.jsp 中 的同名变量冲突
                    java.util.List<String> passwordFields = new ArrayList<>();
                    String echoGroup = "";
                    String fieldValue = value;
                    java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(fieldInfo.getSeparator()));
                    String fieldName = field;

                    if (fieldInfo.getInputType() == InputType.bool) {
                %>
                <%@ include file="../fragment/field_type/bool.jsp" %>
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
                } else {
                    String refModelName = null;
                    String refFieldName = null;
                    String refValue;
                    if (Utils.notBlank(fieldInfo.getLinkList())) {
                        String[] split = fieldInfo.getLinkList().split("\\.");
                        refModelName = split[0];
                        refFieldName = split[1];
                    } else if (Utils.notBlank(fieldInfo.getRefModel())) {
                        refModelName = fieldInfo.getRefModel();
                        refFieldName = SystemController.getModelInfo(qzApp, refModelName).getIdField();
                    }
                    if (Utils.notBlank(value) && refModelName != null && refFieldName != null) {
                        ModelFieldInfo refFieldInfo = SystemController.getModelInfo(qzApp, refModelName).getModelFieldInfo(refFieldName);
                        if (Utils.notBlank(fieldInfo.getLinkList())) {
                            //TE: linkList 传 id 的值
                            refValue = modelData[modelInfo.getFieldIndex(idField)];
                        } else {
                            //应用跳转到实例：refmodel 传点击字段的值
                            refValue = value.replace(fieldInfo.getSeparator(), refFieldInfo.getSeparator());
                        }
                %>
                <a href='<%=PageUtil.buildCustomUrl(request, response, qzRequest,HtmlView.FLAG, refModelName, qingzhou.api.type.List.ACTION_LIST + "?" + refFieldName + "=" + refValue)%>'
                   class="dataid qz-action-link tooltips"
                   data-tip='<%=I18n.getModelI18n(qzApp, "model." + refModelName)%>' data-tip-arrow="top"
                   style="color:#4C638F;" onclick='difModelActive("<%=qzRequest.getModel()%>","<%=refModelName%>")'>
                    <%=PageUtil.styleFieldValue(value, fieldInfo, modelInfo)%>
                </a>
                <%
                        } else {
                            out.print(PageUtil.styleFieldValue(value, fieldInfo, modelInfo));
                        }
                    }
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
                        if (Utils.notBlank(action.getShow())) {
                            showAction = SecurityController.checkRule(action.getShow(), fieldName -> modelData[modelInfo.getFieldIndex(fieldName)]);
                        }
                        if (!showAction) continue;

                        String customActionId = "";
                        if (action.getActionType() == ActionType.sub_form) {
                            customActionId = " custom-action-id='popup-" + qzApp + "-" + qzModel + "-" + action.getCode() + "-" + encodedItemId + "'";
                        }

                        boolean useJsonUri = action.getActionType() == ActionType.sub_form
                                || action.getActionType() == ActionType.action_list
                                || action.getActionType() == ActionType.download;
                %>
                <a href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, useJsonUri ? JsonView.FLAG : HtmlView.FLAG, actionName + "/" + encodedItemId)%>"
                   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionName)%>'
                   data-tip-arrow="top" action-id="<%=qzApp + "-" + qzModel + "-" + actionName%>"
                   class="qz-action-link tooltips" model-icon="<%=modelInfo.getIcon()%>"
                   data-id="<%=(qzModel + "|" + encodedItemId)%>" action-type="<%=action.getActionType()%>"
                   data-name="<%=originUnEncodedId%>"
                        <%
                            if (action.getActionType() == ActionType.download) {
                                out.print(" downloadfile='" + PageUtil.buildRequestUrl(request, response, qzRequest, DownloadView.FLAG, Download.ACTION_DOWNLOAD + "/" + encodedItemId) + "'");
                            }

                            if (Utils.notBlank(customActionId)) {
                                out.print(customActionId);
                                out.print(" form-loaded-trigger=" + action.isFormLoadedTrigger());
                            }

                            if (action.getActionType() == ActionType.sub_form) {
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
                    if (action.getActionType() == ActionType.sub_form) {
                        Map<String, String> actionFormData = new LinkedHashMap<>();
                        for (String fieldName : modelInfo.getFormFieldNames()) {
                            if (Utils.contains(action.getFormFields(), fieldName)) {
                                actionFormData.put(fieldName, modelData[modelInfo.getFieldIndex(fieldName)]);
                            }
                        }
                %>
                <div style="display: none"
                     custom-action-id="popup-<%=qzApp + "-" + qzModel + "-" + action.getCode() + "-" + encodedItemId%>">
                    <%@ include file="../fragment/action_form.jsp" %>
                </div>
                <%
                        }
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

    <div style="text-align: center; <%=(totalSize < 1) ? "display:none;" : ""%>">
        <ul class="pager pager-loose" data-ride="pager" data-page="<%=pageNum%>"
            recPerPage="<%=pageSize%>"
            data-rec-total="<%=totalSize%>"
            partLinkUri="<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, qingzhou.api.type.List.ACTION_LIST + "?markForAddCsrf")%>&<%="pageNum"%>="
            style="margin-left:33%;color:black;margin-bottom:6px;">
        </ul>
    </div>
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

    function upload(file, url, id) {
        const formData = new FormData();
        formData.append(id, file);
        $.ajax({
            url: url,
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function (data) {
                if (data.success === "true" || data.success === true) {
					var searchBtn = $(".filter_search", getRestrictedArea());
					if (searchBtn.length > 0) {
						searchBtn.trigger('click'); //点击搜索按钮，请求list
					} else {
						$("li.treeview.active", getRestrictedArea()).find("a").trigger('click');//点击当前所在菜单，请求list
					}
				}
                showMsg(data.msg, data.msg_level);
            },
            error: function (e) {
                handleError(e);
            }
        });
    }
</script>
