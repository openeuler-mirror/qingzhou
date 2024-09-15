<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    boolean isEdit = Objects.equals(Updatable.ACTION_EDIT, qzAction);
    String submitActionName = isEdit ? Updatable.ACTION_UPDATE : Addable.ACTION_ADD;
    List<String> passwordFields = new ArrayList<>();
    String idFieldName = modelInfo.getIdFieldName();
%>

<%-- <div class="bodyDiv"> --%>
<%-- 面包屑分级导航 --%>
<%@ include file="../fragment/breadcrumb.jsp" %>

<form name="pageForm" method="post" class="form-horizontal"
      action="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, DeployerConstants.jsonView, submitActionName + (isEdit && Utils.notBlank(encodedId) ? "/" + encodedId: ""))%>">
    <div class="block-bg" style="padding-top: 24px; padding-bottom: 1px;">
        <%
            Map<String, String> model = null;
            List<Map<String, String>> models = qzResponse.getDataList();
            if (!models.isEmpty()) {
                model = models.get(0);
                Map<String, Map<String, ModelFieldInfo>> fieldMapWithGroup = PageBackendService.getGroupedModelFieldMap(qzRequest);
                Set<String> groups = fieldMapWithGroup.keySet();
                long suffixId = System.currentTimeMillis();
                if (groups.size() > 1) {
        %>
        <ul class="nav nav-tabs">
            <%
                boolean isFirst = true;
                for (String group : groups) {
                    group = "".equals(group) ? "OTHERS" : group;
                    String finalGroup = group;%>
            <li <%=isFirst ? "class='active'" : ""%>>
                <a data-tab href="#group-<%=group%>-<%=suffixId%>"
                   tabGroup="<%=group%>">
                    <%=I18n.getStringI18n((Arrays.stream(modelInfo.getGroupInfos())
                            .filter(groupInfo -> groupInfo.getName().equals(finalGroup))
                            .findFirst().get().getI18n()))%>
                </a>
            </li>
            <%
                    isFirst = false;
                }
            %>
        </ul>
        <div class="tab-content" style="padding-top: 24px; padding-bottom: 12px;">
            <%
                }
                boolean isFirstGroup = true;
                for (String group : groups) {
            %>
            <div class="tab-pane <%=isFirstGroup?"active":""%>"
                 id="group-<%=("".equals(group) ? "OTHERS":group)%>-<%=suffixId%>"
                 tabGroup="<%=("".equals(group) ? "OTHERS":group)%>">
                <%
                    isFirstGroup = false;
                    Map<String, ModelFieldInfo> groupFieldMap = fieldMapWithGroup.get(group);
                    if (groupFieldMap == null) {
                        continue;
                    }
                    for (Map.Entry<String, ModelFieldInfo> e : groupFieldMap.entrySet()) {
                        ModelFieldInfo modelField = e.getValue();
                        if (!modelField.isCreateable() && !isEdit) {
                            continue;
                        }
                        /*if (!modelField.isEditable() && isEdit) {
                            continue;
                        }*/

                        String fieldName = e.getKey();

                       /* if (modelField.clientEncrypt()) {
                            passwordFields.add(fieldName);
                        }*/

                        String readonly = "";
                        if (!modelField.isEditable() && isEdit) {
                            readonly = "readonly";
                        }
                        if (fieldName.equals(idFieldName)) {
                            if (isEdit) {
                                readonly = "readonly";
                            }
                        }
                        if (PageBackendService.isFieldReadOnly(qzRequest, fieldName)) {
                            readonly = "readonly";
                        }
                        boolean required = fieldName.equals(idFieldName) || modelField.isRequired();

                        String fieldValue = String.valueOf(model.get(fieldName));// 需要在 isFieldReadOnly 之后，原因是 license 限制的 5 个并发会在其中被修改，总之最后读取值是最新的最准确的
                        List<String> fieldValues = fieldValue == null ? new ArrayList<>() : Arrays.asList(fieldValue.split(","));
                        if (fieldValue == null || fieldValue.equals("null")) {
                            fieldValue = "";
                        }
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
                            FieldType fieldType = FieldType.valueOf(modelField.getType());
                            switch (fieldType) {
                                case text:
                        %>
                        <input type="text" name="<%=fieldName%>" value='<%=fieldValue%>'
                               class="form-control">
                        <%
                                break;
                            case number:
                        %>
                        <input type="number" min="<%=modelField.getMin()%>"
                               max="<%=(modelField.isPort()?"65535":modelField.getMax())%>"
                               name="<%=fieldName%>" value='<%=fieldValue%>' class="form-control">
                        <%
                                break;
                            case decimal:%>
                        <%@ include file="field_type/decimal.jsp" %>
                        <%
                                break;
                            case password:
                                passwordFields.add(fieldName);
                        %>
                        <input type="password" name="<%=fieldName%>" value='<%=fieldValue%>' data-type="password"
                               class="form-control">
                        <label password_label_right="<%=fieldName%>_eye" class="input-control-icon-right"
                               style="margin-right: 10px; cursor: pointer;"><i class="icon icon-eye-close"></i></label>
                        <%
                                break;
                            case textarea:
                        %>
                        <textarea name="<%=fieldName%>" value='<%=fieldValue%>' class="form-control"
                                  rows="3"><%=fieldValue%></textarea>
                        <%
                                break;
                            case markdown:
                        %>
                        <div class="markedview"></div>
                        <textarea name="<%=fieldName%>" class="markedviewText" rows="3"><%=fieldValue%></textarea>
                        <%
                                break;
                            case radio:%>
                        <%@ include file="field_type/radio.jsp" %>
                        <%
                                break;
                            case bool:
                        %>
                        <%@ include file="field_type/bool.jsp" %>
                        <%
                                break;
                            case select:
                        %>
                        <%@ include file="field_type/select.jsp" %>
                        <%
                                break;
                            case multiselect:
                        %>
                        <%@ include file="field_type/multiselect.jsp" %>
                        <%
                                break;
                            case checkbox:
                        %>
                        <%@ include file="field_type/checkbox.jsp" %>
                        <%
                                break;
                            case sortablecheckbox:
                        %>
                        <%@ include file="field_type/sortablecheckbox.jsp" %>
                        <%
                                break;
                            case file:
                        %>
                        <%@ include file="field_type/file.jsp" %>
                        <%
                                break;
                            case sortable:
                        %>
                        <%@ include file="field_type/sortable.jsp" %>
                        <%
                                break;
                            case kv:
                        %>
                        <%@ include file="field_type/kv.jsp" %>
                        <%
                                break;
                            case datetime:
                        %>
                        <%@ include file="field_type/datetime.jsp" %>
                        <%
                                        break;
                                    default:
                                        throw new IllegalStateException(modelField.getType() + ".jsp not found.");
                                }
                            }
                        %>
                        <label class="tw-error-info"></label>
                    </div>
                </div>
                <%
                    }
                    if (groups.size() > 1) {
                %>
            </div>
            <%
                        }
                    }
                }
            %>
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

                    if (SecurityController.isActionShow(qzApp, qzModel, Listable.ACTION_LIST, model, currentUser)) {
                %>
                <a href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, Listable.ACTION_LIST)%>"
                   btn-type="goback" class="btn">
                    <%=I18n.getKeyI18n("page.return")%>
                </a>
                <%
                    }

                    if (modelInfo.getModelActionInfo(DeployerConstants.ACTION_REFRESHKEY) != null) {
                %>
                <a href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.imageView, DeployerConstants.ACTION_REFRESHKEY)%>"
                   btn-type="qrOtp" class="btn">
                    <%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + DeployerConstants.ACTION_REFRESHKEY)%>
                </a>
                <%
                    }

                    if (SecurityController.isActionShow(qzApp, qzModel, Downloadable.ACTION_DOWNLOAD, model, currentUser)) {
                %>
                <a href='<%=PageBackendService.buildRequestUrl(request, response, qzRequest, DeployerConstants.jsonView, Downloadable.ACTION_FILES + (Utils.notBlank(encodedId) ? "/" + encodedId : ""))%>'
                        <%
                            out.print(" downloadfile='" + PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.fileView, "download" + (Utils.notBlank(encodedId) ? "/" + encodedId : "")) + "'");
                        %>
                   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + Downloadable.ACTION_FILES)%>'
                   data-tip-arrow="top"
                   btn-type="<%=Downloadable.ACTION_FILES%>" class="btn tooltips">
                    <%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + Downloadable.ACTION_FILES)%>
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
            Map<String, String> showConditions = PageBackendService.modelFieldShowMap(qzRequest);
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
            Map<String, String> readonlyConditions = PageBackendService.modelFieldReadOnlyMap(qzRequest);
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
    </div>
</form>
<%-- </div> --%>
