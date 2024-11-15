<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    boolean isEdit = Objects.equals(Update.ACTION_EDIT, qzAction);
    String submitActionName = isEdit ? Update.ACTION_UPDATE : Add.ACTION_ADD;
    java.util.List<String> passwordFields = new ArrayList<>();
    String idField = modelInfo.getIdField();
    String[] formActions = PageUtil.filterActions(modelInfo.getFormActions(), qzApp, qzModel, currentUser);

    Map<String, String> modelData = (Map<String, String>) qzResponse.getInternalData();
    Map<String, List<String>> sameLineMap = modelInfo.getSameLineMap();
    Map<String, String> sameLineModelData = new LinkedHashMap<>();
    sameLineMap.values().stream().flatMap((Function<List<String>, Stream<String>>) Collection::stream).forEach((Consumer<String>) f -> sameLineModelData.put(f, modelData.remove(f)));

    Map<String, List<String>> groupedFields = PageUtil.groupedFields(modelData.keySet(), modelInfo);
    boolean hasGroup = PageUtil.hasGroup(groupedFields);
%>

<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <form name="pageForm" method="post" class="form-horizontal"
          action="<%=PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG, submitActionName + (isEdit && Utils.notBlank(encodedId) ? "/" + encodedId: ""))%>">
        <div style="padding-top: 24px; padding-bottom: 1px;">
            <%
                Set<String> groups = groupedFields.keySet();
                long suffixId = System.currentTimeMillis();
                ItemInfo[] groupInfos = modelInfo.getGroupInfos();
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
                     id="group-<%=group%>-<%=suffixId%>" tabGroup="<%=group%>">
                    <%
                        isFirstGroup = false;
                        String echoGroup = null;
                        boolean isDisabled = false;
                        for (String fieldName : groupedFields.get(group)) {
                            List<String> sameLineFields = sameLineMap.get(fieldName);

                            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
                            if (isEdit) {
                                if (!modelField.isEdit()) continue;
                            } else {
                                if (!modelField.isCreate()) continue;
                            }

                            String needHidden = "";
                            if (modelField.isHidden()) {
                                needHidden = "display:none";
                            }
                            boolean required = fieldName.equals(idField) || modelField.isRequired();

                            String fieldValue = modelData.get(fieldName);
                            if (fieldValue == null) {
                                fieldValue = "";
                            }
                            java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(modelField.getSeparator()));
                    %>
                    <div style="<%=needHidden%>" class="form-group" id="form-item-<%=fieldName%>">
                        <label class="col-sm-4">
                            <%
                                if (modelField.isShowLabel()) {
                            %>
                            <%=required ? "<span  style=\"color:red;\">* </span>" : ""%>
                            <%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>
                            <%
                                String fieldInfo = I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName);
                                if (Utils.notBlank(fieldInfo)) {
                                    // 注意：下面这个 title=xxxx 必须使用单引号，因为 Model 的注解里面用了双引号，会导致显示内容被截断!
                            %>
                            <span class='tooltips' data-tip='<%=fieldInfo%>' data-tip-arrow='bottom-right'>
								<i class='icon icon-info-sign'></i>
							</span>
                            <%
                                    }
                                }
                            %>
                        </label>
                        <div class="col-sm-<%=sameLineFields!=null ? 3/sameLineFields.size() : 5%>"
                             type="<%=modelField.getInputType().name()%>">
                            <%@ include file="../fragment/field_type.jsp" %>
                            <label class="qz-error-info"></label>
                        </div>

                        <%
                            if (sameLineFields != null) {
                                for (String sameLineField : sameLineFields) {
                                    fieldName = sameLineField;
                                    modelField = modelInfo.getModelFieldInfo(fieldName);
                                    needHidden = "";
                                    if (modelField.isHidden()) {
                                        needHidden = "display:none";
                                    }
                                    required = fieldName.equals(idField) || modelField.isRequired();

                                    fieldValue = sameLineModelData.get(fieldName);
                                    if (fieldValue == null) {
                                        fieldValue = "";
                                    }
                                    fieldValues = Arrays.asList(fieldValue.split(modelField.getSeparator()));

                        %>
                        <div style="<%=needHidden%>" id="form-item-<%=fieldName%>">
                            <%
                                if (modelField.isShowLabel()) {
                            %>
                            <label for="<%=fieldName%>" class="col-sm-1">
                                <%=required ? "<span  style=\"color:red;\">* </span>" : ""%>
                                <%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>
                                <%
                                    String fieldInfo = I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName);
                                    if (Utils.notBlank(fieldInfo)) {
                                        // 注意：下面这个 title=xxxx 必须使用单引号，因为 Model 的注解里面用了双引号，会导致显示内容被截断!
                                %>
                                <span class='tooltips' data-tip='<%=fieldInfo%>' data-tip-arrow='bottom-right'>
								<i class='icon icon-info-sign'></i>
								</span>
                                <%
                                    }
                                %>
                            </label>
                            <%
                                }
                            %>
                            <div class="col-sm-<%=(5-3/sameLineFields.size())/sameLineFields.size()%>"
                                 type="<%=modelField.getInputType().name()%>">
                                <%@ include file="../fragment/field_type.jsp" %>
                                <label class="qz-error-info"></label>
                            </div>
                        </div>
                        <%
                                }
                            }
                        %>
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
                        boolean actionPermitted = SecurityController.isActionPermitted(qzApp, qzModel, formAction, currentUser);
                        if (!actionPermitted) continue;
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


                    if (SecurityController.isActionPermitted(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, currentUser)) {
                %>
                <a class="btn" modelname="<%=qzModel%>"
                   href="javascript:void(0);" onclick="returnHref(this);">
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
