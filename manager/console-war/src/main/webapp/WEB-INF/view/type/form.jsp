<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
	boolean isEdit = Objects.equals(Update.ACTION_EDIT, qzAction);
	String submitActionName = isEdit ? Update.ACTION_UPDATE : Add.ACTION_ADD;
	java.util.List<String> passwordFields = new ArrayList<>();
	String idField = modelInfo.getIdField();
	String[] formActions = PageUtil.filterActions(modelInfo.getFormActions(), qzApp, qzModel, currentUser);
%>

<div class="bodyDiv">
	<%-- 面包屑分级导航 --%>
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<form name="pageForm" method="post" class="form-horizontal"
		  action="<%=PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG, submitActionName + (isEdit && Utils.notBlank(encodedId) ? "/" + encodedId: ""))%>">
		<div style="padding-top: 24px; padding-bottom: 1px;">
			<%
				Map<String, String> modelData = qzResponse.getDataMap();
				Map<String, Map<String, ModelFieldInfo>> formGroup = modelInfo.getFormGroupedFields();
				Set<String> groups = formGroup.keySet();
				long suffixId = System.currentTimeMillis();
				ItemInfo[] groupInfos = modelInfo.getGroupInfos();
				boolean hasGroup = groups.size() > 1 || (groups.size() == 1 && Utils.notBlank(groups.iterator().next()));
				if (hasGroup) {
			%>
			<ul class="nav nav-tabs">
				<%
					boolean isFirst = true;
					for (String group : groups) {
						ItemInfo gInfo = Arrays.stream(groupInfos).filter(groupInfo -> groupInfo.getName().equals(group)).findAny().orElse(PageUtil.OTHER_GROUP);
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

							if (isEdit) {
								if (!modelField.isEdit()) continue;
							} else {
								if (!modelField.isCreate()) continue;
							}

							boolean readOnly = fieldName.equals(idField) && isEdit;
							if (!readOnly) {
								readOnly = modelField.isReadonly();
							}

							boolean required = fieldName.equals(idField) || modelField.isRequired();

							String echoGroup = "";
							if (modelField.getEchoGroup().length > 0) {
								String echoGroups = String.join(",", modelField.getEchoGroup());
								echoGroup = "echoGroup='" + echoGroups + "'";
							}

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
						<div class="col-sm-5" type="<%=modelField.getInputType().name()%>">
							<%
								if (readOnly) {
									if (fieldValue.isEmpty()){
										fieldValue = modelField.getDefaultValue();
									}
							%>
							<input type="text" disabled="disabled" name="<%=fieldName%>"
								   value='<%=fieldValue%>' <%=echoGroup%>
								   class="form-control"/>
							<%
							} else if (modelField.isReadonlyStyle()){
							%>
							<input type="text" readonly name="<%=fieldName%>"
								   style="cursor: not-allowed;border: 1px solid #DCDCDC;background-color: #e5e5e5;"
								   value='<%=fieldValue%>' <%=echoGroup%>
								   class="form-control"/>
							<%
							}else {
							%>
							<%@ include file="../fragment/field_type.jsp" %>
							<%
								}
							%>
							<label class="qz-error-info"></label>
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
		<div style="margin-top: 15px; height: 64px; text-align: center;">
			<div class="form-btn">
				<%
					if (SecurityController.isActionPermitted(qzApp, qzModel, submitActionName, currentUser)) {
				%>
				<input type="submit" class="btn"
					   value='<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + submitActionName)%>'>
				<%
					}

					for (String formAction : formActions) {
						ModelActionInfo formActionInfo = modelInfo.getModelActionInfo(formAction);
						if (formActionInfo.getActionType() == ActionType.qr) {
				%>
				<a href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, DownloadView.FLAG, formAction)%>"
				   btn-type="qrOtp" class="btn">
					<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + formAction)%>
				</a>
				<%
				} else if (formActionInfo.getActionType() == ActionType.download) {
				%>
				<a href='<%=PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG, formAction + (Utils.notBlank(encodedId) ? "/" + encodedId : ""))%>'
						<%
							out.print(" downloadfile='" + PageUtil.buildRequestUrl(request, response, qzRequest, DownloadView.FLAG, Download.ACTION_DOWNLOAD + (Utils.notBlank(encodedId) ? "/" + encodedId : "")) + "'");
						%>
				   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + formAction)%>'
				   data-tip-arrow="top" class="btn tooltips">
					<%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + formAction)%>
				</a>
				<%
						}
					}
				%>

				<%
					if (SecurityController.isActionPermitted(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, currentUser)) {
				%>
				<a class="btn"
				   onclick="returnHref('<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, qingzhou.api.type.List.ACTION_LIST)%>')"
				   href="javascript:void(0)">
					<%=I18n.getKeyI18n("page.return")%>
				</a>
				<%
					}
				%>
			</div>
		</div>

		<div id="tempZone" style="display:none;"></div>

		<textarea name="pubkey" rows="3" disabled="disabled"
				  style="display:none;"><%=SystemController.getPublicKeyString()%></textarea>

		<%
			// added by yuanwc for: ModelField 注解 show()
			StringBuilder displayCondition = new StringBuilder();
			displayCondition.append("{");
			boolean isFirst = true;
			for (Map.Entry<String, String> entry : modelInfo.getFormFieldDisplay().entrySet()) {
				if (!isFirst) displayCondition.append(",");
				isFirst = false;

				displayCondition.append("\"").append(entry.getKey()).append("\":")
						.append("\"").append(entry.getValue().replaceAll("\\&\\&", "&").replaceAll("\\|\\|", "|")).append("\"");
			}
			displayCondition.append("}");
		%>
		<textarea name="showCondition" rows="3" disabled="disabled"
				  style="display:none;"><%=displayCondition.toString()%></textarea>

		<%
			StringBuilder pwdFields = new StringBuilder();
			for (int i = 0; i < passwordFields.size(); i++) {
				if (i > 0) {
					pwdFields.append(",");
				}
				pwdFields.append(passwordFields.get(i));
			}
		%>
		<textarea name="passwordFields" rows="3" disabled="disabled"
				  style="display:none;"><%=pwdFields.toString()%></textarea>

	</form>

</div>