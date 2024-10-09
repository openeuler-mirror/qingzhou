<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
	boolean isEdit = Objects.equals(Update.ACTION_EDIT, qzAction);
	String submitActionName = isEdit ? Update.ACTION_UPDATE : Add.ACTION_ADD;
	java.util.List<String> passwordFields = new ArrayList<>();
	String idField = modelInfo.getIdField();
%>

<%-- <div class="bodyDiv"> --%>
<%-- 面包屑分级导航 --%>
<%@ include file="../fragment/breadcrumb.jsp" %>

<form name="pageForm" method="post" class="form-horizontal"
	  action="<%=PageUtil.buildRequestUrl(request, response, qzRequest, DeployerConstants.JSON_VIEW, submitActionName + (isEdit && Utils.notBlank(encodedId) ? "/" + encodedId: ""))%>">
	<div class="block-bg" style="padding-top: 24px; padding-bottom: 1px;">
		<%
			java.util.List<Map<String, String>> models = qzResponse.getDataList();
			Map<String, String> modelData;
			if (!models.isEmpty()) {
				modelData = models.get(0);
			} else {
				modelData = new HashMap<>();
			}
			Map<String, Map<String, ModelFieldInfo>> formGroup = modelInfo.getFormGroupedFields();
			Set<String> groups = formGroup.keySet();
			long suffixId = System.currentTimeMillis();
			GroupInfo[] groupInfos = modelInfo.getGroupInfos();
			boolean hasGroup = groups.size() > 1 || Utils.notBlank(groups.iterator().next());
			if (hasGroup) {
		%>
		<ul class="nav nav-tabs">
			<%
				boolean isFirst = true;
				for (String group : groups) {
					GroupInfo gInfo = Arrays.stream(groupInfos).filter(groupInfo -> groupInfo.getName().equals(group)).findAny().orElse(PageUtil.OTHER_GROUP);
			%>
			<li <%=isFirst ? "class='active'" : ""%>>
				<a data-tab href="#group-<%=group%>-<%=suffixId%>"
				   tabGroup="<%=group%>">
					<%=I18n.getStringI18n(gInfo.getI18n())%>
				</a>
			</li>
			<%
					isFirst = false;
				}
			%>
		</ul>
		<%
			}
		%>
		<div class="tab-content" style="padding-top: 24px; padding-bottom: 12px;">
			<%
				boolean isFirstGroup = true;
				for (String group : groups) {
			%>
			<div class="tab-pane <%=isFirstGroup?"active":""%>"
				 id="group-<%=group%>-<%=suffixId%>"
				 tabGroup="<%=group%>">
				<%
					isFirstGroup = false;
					Map<String, ModelFieldInfo> groupFieldMap = formGroup.get(group);
					for (Map.Entry<String, ModelFieldInfo> e : groupFieldMap.entrySet()) {
						String fieldName = e.getKey();
						ModelFieldInfo modelField = e.getValue();

						if (!modelField.isCreateable() && !isEdit) {
							continue;
						}

						String readonly = "";
						if (!modelField.isEditable() && isEdit) {
							readonly = "readonly";
						} else if (fieldName.equals(idField)) {
							if (isEdit) {
								readonly = "readonly";
							}
						} else {
							boolean readOnly = SecurityController.checkRule(modelField.getReadOnly(), modelData::get, false);
							if (readOnly) {
								readonly = "readonly";
							}
						}

						boolean required = fieldName.equals(idField) || modelField.isRequired();

						String fieldValue = modelData.get(fieldName);
						if (fieldValue == null) {
							fieldValue = "";
						}
						java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(modelField.getSeparator()));
				%>
				<div class="form-group" id="form-item-<%=fieldName%>">
					<label for="<%=fieldName%>" class="col-sm-4">
						<%=required ? "<span  style=\"color:red;\">* </span>" : ""%>
						<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>
						<%
							String fieldInfo = I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName);
							if (fieldInfo != null) {
								// 注意：下面这个 title=xxxx 必须使用单引号，因为 Model 的注解里面用了双引号，会导致显示内容被截断!
								fieldInfo = "<span class='tooltips' data-tip='" + fieldInfo + "' data-tip-arrow='bottom-right'><i class='icon icon-question-sign'></i></span>";
							} else {
								fieldInfo = "";
							}
						%>
						<%=fieldInfo%>
					</label>
					<div class="col-sm-5">
						<%
							if (!readonly.isEmpty()) {
								if (FieldType.textarea.name().equals(modelField.getType())) {
						%>
						<textarea rows="3" disabled="disabled" name="<%=fieldName%>" class="form-control"
								  readonly="readonly"><%=fieldValue%></textarea>
						<%
						} else {
						%>
						<input type="text" name="<%=fieldName%>" value='<%=fieldValue%>'
							   class="form-control" readonly="readonly">
						<%
							}
						} else {
						%>
						<%@ include file="../fragment/field_type.jsp" %>
						<%
							}
						%>
						<label class="tw-error-info"></label>
					</div>
				</div>
				<%
					}
				%>
			</div>
			<%
				}
			%>
		</div>
	</div>
	<div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
		<div class="form-btn">
			<%
				if (SecurityController.isActionShow(qzApp, qzModel, submitActionName, null, currentUser)) {
			%>
			<input type="submit" class="btn"
				   value='<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + submitActionName)%>'>
			<%
				}

				if (SecurityController.isActionShow(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, modelData, currentUser)) {
			%>
			<a href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, qingzhou.api.type.List.ACTION_LIST)%>"
			   btn-type="goback" class="btn">
				<%=I18n.getKeyI18n("page.return")%>
			</a>
			<%
				}

				if (modelInfo.getModelActionInfo(DeployerConstants.ACTION_REFRESHKEY) != null) {
			%>
			<a href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, ViewManager.imageView, DeployerConstants.ACTION_REFRESHKEY)%>"
			   btn-type="qrOtp" class="btn">
				<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + DeployerConstants.ACTION_REFRESHKEY)%>
			</a>
			<%
				}

				if (SecurityController.isActionShow(qzApp, qzModel, Download.ACTION_DOWNLOAD, modelData, currentUser)) {
			%>
			<a href='<%=PageUtil.buildRequestUrl(request, response, qzRequest, DeployerConstants.JSON_VIEW, Download.ACTION_FILES + (Utils.notBlank(encodedId) ? "/" + encodedId : ""))%>'
					<%
						out.print(" downloadfile='" + PageUtil.buildRequestUrl(request, response, qzRequest, ViewManager.fileView, "download" + (Utils.notBlank(encodedId) ? "/" + encodedId : "")) + "'");
					%>
			   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + Download.ACTION_FILES)%>'
			   data-tip-arrow="top"
			   btn-type="<%=Download.ACTION_FILES%>" class="btn tooltips">
				<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + Download.ACTION_FILES)%>
			</a>
			<%
				}
			%>

		</div>
	</div>

	<div id="tempZone" style="display:none;"></div>
	<textarea name="pubkey" rows="3" disabled="disabled" style="display:none;">
        <%=SystemController.getPublicKeyString()%>
        </textarea>

	<textarea name="eventConditions" rows="3" disabled="disabled" style="display:none;">
        <%
			// added by yuanwc for: ModelField 注解 show()
			StringBuilder conditionBuilder = new StringBuilder();
			conditionBuilder.append("{");

			// 处理 show 条件
			conditionBuilder.append("\"show\": {");
			Map<String, String> showConditions = modelInfo.getShowMap();
			for (Map.Entry<String, String> entry : showConditions.entrySet()) {
				conditionBuilder.append("\"").append(entry.getKey()).append("\": \"")
						.append(entry.getValue().replaceAll("\\&\\&", "&").replaceAll("\\|\\|", "|")).append("\",");
			}
			if (conditionBuilder.lastIndexOf(",") > 0) {
				conditionBuilder.deleteCharAt(conditionBuilder.lastIndexOf(","));
			}
			conditionBuilder.append("},");

			// 处理 readonly 条件
			conditionBuilder.append("\"readonly\": {");
			Map<String, String> readonlyConditions = modelInfo.getReadOnlyMap();
			for (Map.Entry<String, String> entry : readonlyConditions.entrySet()) {
				conditionBuilder.append("\"").append(entry.getKey()).append("\": \"")
						.append(entry.getValue()).append("\",");
			}
			if (!readonlyConditions.isEmpty()) {
				conditionBuilder.deleteCharAt(conditionBuilder.lastIndexOf(","));
			}
			conditionBuilder.append("}");

			conditionBuilder.append("}");
			out.print(conditionBuilder.toString());
		%>
        </textarea>
	<textarea name="passwordFields" rows="3" disabled="disabled" style="display:none;">
        <%
			for (int i = 0; i < passwordFields.size(); i++) {
				if (i > 0) {
					out.print(",");
				}
				out.print(passwordFields.get(i));
			}
		%>
        </textarea>

</form>
