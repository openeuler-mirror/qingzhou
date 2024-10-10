<%@ page pageEncoding="UTF-8" %>

<%@ include file="../fragment/head.jsp" %>

<%
	String contextPath = request.getContextPath();
	String idField = modelInfo.getIdField();
	String[] fieldsToList = modelInfo.getFieldsToList();

	String[] listActions = modelInfo.getListActionNames();
	ModelActionInfo[] batchActions = PageUtil.listBachActions(qzRequest, qzResponse, currentUser);

	int totalSize = qzResponse.getTotalSize();
	int pageNum = qzResponse.getPageNum();
	int pageSize = qzResponse.getPageSize();
%>

<div class="bodyDiv">
	<%-- 面包屑分级导航 --%>
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<div class="block-bg">
		<%@ include file="../fragment/filter_form.jsp" %>

		<hr style="margin-top: 4px;">

		<div class="table-tools tw-list-operate">
			<div class="tools-group">
				<%
					for (String actionName : modelInfo.getHeadActionNames()) {
						if (!SecurityController.isActionShow(qzApp, qzModel, actionName, null, currentUser)) {
							continue;
						}
						ModelActionInfo action = modelInfo.getModelActionInfo(actionName);

						String customActionId = "";
						if (action.getFields() != null && action.getFields().length > 0) {
							customActionId = " custom-action-id='popup-" + qzApp + "-" + qzModel + "-" + action.getCode() + "'";
						}

				%>
				<a class="btn" data-tip-arrow="top" action-name="<%=actionName%>" <%=customActionId%>
				   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionName)%>'
				   href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, Utils.notBlank(customActionId)?DeployerConstants.JSON_VIEW:ViewManager.htmlView, actionName)%>"
				>
					<i class="icon icon-<%=action.getIcon()%>"></i>
					<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionName)%>
				</a>
				<%
					if (Utils.notBlank(customActionId)) {
						Map<String, String> modelData = new HashMap<>();
				%>
				<div style="display: none" <%=customActionId%>>
					<%@ include file="../fragment/action_form.jsp" %>
				</div>
				<%
						}
					}

					// 支持批量操作的按钮
					for (ModelActionInfo action : batchActions) {
						String actionKey = action.getCode();
						if (!SecurityController.isActionShow(qzApp, qzModel, actionKey, null, currentUser)) {
							continue;
						}
						String operationConfirm = String.format(I18n.getKeyI18n("page.operationConfirm"),
								I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey),
								I18n.getModelI18n(qzApp, "model." + qzModel));

						String actionUrl = PageUtil.buildRequestUrl(request, response, qzRequest, DeployerConstants.JSON_VIEW, actionKey);
				%>
				<a id="<%=actionKey%>" action-name="<%=actionKey%>"
				   href="<%=actionUrl%>"
				   onclick='batchOps("<%=actionUrl%>","<%=actionKey%>")'
				   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionKey)%>'
				   class="btn batch-ops"
				   disabled="disabled" model-icon="<%=modelInfo.getIcon()%>"
				   data-name="" data-id="" act-ajax='true' act-confirm='<%=operationConfirm%> ?'>
					<i class="icon icon-<%=action.getIcon()%>"></i>
					<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey)%>
				</a>
				<%
					}
				%>
			</div>
		</div>

		<table class="table table-striped table-hover list-table responseScroll">
			<thead>
			<tr style="height:20px;">
				<%
					if (batchActions.length > 0) {
				%>
				<th class="custom-checkbox">
					<input type="checkbox" class="allcheck"/>
				</th>
				<%
					}
				%>
				<th class="sequence"><%=I18n.getKeyI18n("page.list.order")%>
				</th>
				<%
					for (String field : fieldsToList) {
				%>
				<th><%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + field)%>
				</th>
				<%
					}
					if (listActions.length > 0) {
						out.print("<th>" + I18n.getKeyI18n("page.action") + "</th>");
					}
				%>
			</tr>
			</thead>
			<tbody>
			<%
				java.util.List<Map<String, String>> modelDataList = qzResponse.getDataList();
				if (modelDataList.isEmpty()) {
					String dataEmpty = "<tr><td colspan='" + (1 + fieldsToList.length + (listActions.length > 0 ? 1 : 0)) + "' align='center'>"
							+ "<img src='" + contextPath + "/static/images/data-empty.svg' style='width:160px; height: 160px;'><br>"
							+ "<span style='font-size:14px; font-weight:600; letter-spacing: 2px;'>" + I18n.getKeyI18n("page.none") + "</span></td>";
					out.print(dataEmpty);
				} else {
					int listOrder = (pageNum - 1) * pageSize;
					for (Map<String, String> modelData : modelDataList) {
						String originUnEncodedId = modelData.get(idField);
						String encodedItemId = RESTController.encodeId(originUnEncodedId);
			%>
			<tr>
				<%
					if (batchActions.length > 0) {
				%>
				<td class="custom-checkbox">
					<input type="checkbox" class='morecheck'
						   value="<%=encodedItemId%>"/>
				</td>
				<%
					}
				%>
				<td class="sequence"><%=++listOrder%>
				</td>
				<%
					boolean isFirst = true;
					for (String field : fieldsToList) {
						ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
						String value = modelData.get(field);
						if (value == null) {
							value = "";
						}
				%>
				<td>
					<%
						if (isFirst) {
							isFirst = false;
							String actionName = SecurityController.isActionShow(qzApp, qzModel, Update.ACTION_EDIT, modelData, currentUser)
									? Update.ACTION_EDIT
									: (
									SecurityController.isActionShow(qzApp, qzModel, Show.ACTION_SHOW, modelData, currentUser)
											? Show.ACTION_SHOW
											: null);
							if (actionName != null) {
					%>

					<a href='<%=PageUtil.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView , actionName + "/" + encodedItemId)%>'
					   class="dataid tooltips"
					   record-action-id="<%=actionName%>"
					   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionName)%>'
					   data-tip-arrow="top"
					   style="color:#4C638F;">
						<%=PageUtil.styleFieldValue(value, fieldInfo)%>
					</a>
					<%
						} else {
							out.print(PageUtil.styleFieldValue(value, fieldInfo));
						}
					} else {
						String refModelName = null;
						String refFieldName = null;
						String refValue;
						if (Utils.notBlank(fieldInfo.getLink())) {
							String[] split = fieldInfo.getLink().split("\\.");
							refModelName = split[0];
							refFieldName = split[1];
						} else if (Utils.notBlank(fieldInfo.getRefModel())) {
							refModelName = fieldInfo.getRefModel();
							refFieldName = SystemController.getModelInfo(qzApp, refModelName).getIdField();
						}
						if (refModelName != null && refFieldName != null) {
							ModelFieldInfo refFieldInfo = SystemController.getModelInfo(qzApp, refModelName).getModelFieldInfo(refFieldName);
							refValue = value.replace(fieldInfo.getSeparator(), refFieldInfo.getSeparator());
					%>
					<a href='<%=PageUtil.buildCustomUrl(request, response, qzRequest,ViewManager.htmlView, refModelName, qingzhou.api.type.List .ACTION_LIST + "?" + refFieldName + "=" + refValue)%>'
					   onclick='difModelActive("<%=qzRequest.getModel()%>","<%=refModelName%>")'
					   class="dataid tooltips" record-action-id="<%=qingzhou.api.type.List.ACTION_LIST%>"
					   data-tip='<%=I18n.getModelI18n(qzApp, "model." + refModelName)%>' data-tip-arrow="top"
					   style="color:#4C638F;">
						<%=PageUtil.styleFieldValue(value, fieldInfo)%>
					</a>
					<%
							} else {
								out.print(PageUtil.styleFieldValue(value, fieldInfo));
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
							if (!SecurityController.isActionShow(qzApp, qzModel, actionName, modelData, currentUser)) {
								continue;
							}
							ModelActionInfo action = modelInfo.getModelActionInfo(actionName);

							String customActionId = "";
							if (action.getFields() != null && action.getFields().length > 0) {
								customActionId = " custom-action-id='popup-" + qzApp + "-" + qzModel + "-" + action.getCode() + "-" + encodedItemId + "'";
							}

							boolean useJsonUri = actionName.equals(Download.ACTION_FILES)
									|| Utils.notBlank(customActionId)
									|| actionName.equals(Delete.ACTION_DELETE)
									|| actionName.equals("start")
									|| actionName.equals("stop");
					%>
					<a href="<%=PageUtil.buildRequestUrl(request, response, qzRequest,
                            useJsonUri ? DeployerConstants.JSON_VIEW : ViewManager.htmlView,
                            actionName + "/" + encodedItemId)%>"
					   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionName)%>'
					   class="tw-action-link tooltips" data-tip-arrow="top"
					   model-icon="<%=modelInfo.getIcon()%>" action-name="<%=actionName%>"
					   data-name="<%=originUnEncodedId%>" data-id="<%=(qzModel + "|" + encodedItemId)%>"
							<%
								if (actionName.equals(Download.ACTION_FILES)) {
									out.print(" downloadfile='" + PageUtil.buildRequestUrl(request, response, qzRequest, ViewManager.fileView, Download.ACTION_DOWNLOAD + "/" + encodedItemId) + "'");
								}

								if (Utils.notBlank(customActionId)) {
									out.print(customActionId);
								}

								if (useJsonUri) {
									out.print(" act-ajax='true' act-confirm='"
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
						if (Utils.notBlank(customActionId)) {
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
				partLinkUri="<%=PageUtil.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, qingzhou.api.type.List.ACTION_LIST + "?markForAddCsrf")%>&<%="pageNum"%>="
				style="margin-left:33%;color:black;margin-bottom:6px;">
			</ul>
		</div>

	</div>
</div>

<script>
    function difModelActive(omodel, nmodel) {
        if (omodel !== nmodel) {
            var omenuItemLink = $("ul.sidebar-menu li a[modelName='" + omodel + "']");
            var nmenuItemLink = $("ul.sidebar-menu li a[modelName='" + nmodel + "']");
            if (omenuItemLink.length > 0) {
                if ($(omenuItemLink).parent().hasClass("treeview")) {
                    $(omenuItemLink).parents("li.treeview").removeClass("active");
                } else {
                    $(omenuItemLink).parents("li.treeview").removeClass("menu-open active");
                }
                $(omenuItemLink).parents("ul.treeview-menu").hide();
                $(omenuItemLink).parent().removeClass("active");
            }
            if (nmenuItemLink.length > 0) {
                if ($(nmenuItemLink).parent().hasClass("treeview")) {
                    $(nmenuItemLink).parents("li.treeview").addClass("active");
                } else {
                    $(nmenuItemLink).parents("li.treeview").addClass("menu-open active");
                }
                $(nmenuItemLink).parents("ul.treeview-menu").show();
                $(nmenuItemLink).parent().addClass("active");
            }
        }
    }

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

    function batchOps(url, action) {
        var params = "";
        $(".list-table  input[type='checkbox'][class='morecheck']", getRestrictedArea()).each(function () {
            if ($(this).prop("checked")) {
                if ($(this).attr("value") !== undefined && $(this).attr("value") !== null && $(this).attr("value") !== "") {
                    params = params + $(this).attr("value") + "<%=DeployerConstants.BATCH_ID_SEPARATOR%>"
                }
            }
        });
        var str = url;
        if (str.indexOf("?") > -1) {
            url = str + "&<%=idField%>=" + params;
        } else {
            url = str + "?<%=idField%>=" + params;
        }
        $("#" + action, getRestrictedArea()).attr("href", url);
    }
</script>
