<%@ page import="qingzhou.registry.ModelFieldInfo" %>
<%@ page import="qingzhou.registry.ModelActionInfo" %>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    if (qzRequest == null || qzResponse == null || modelInfo == null) {
        return; // for 静态源码漏洞扫描
    }

    boolean isEdit = Objects.equals(Editable.ACTION_NAME_EDIT, qzRequest.getAction());
    String submitActionName = PageBackendService.getSubmitActionName(qzRequest);
    String idFieldName = Listable.FIELD_NAME_ID;
    ModelFieldInfo idField = modelInfo.getModelFieldInfo(idFieldName);
    final boolean hasId = idField != null;
    List<String> passwordFields = new ArrayList<>();
    String id = qzRequest.getId();
    String encodedId = PageBackendService.encodeId(id);

    if (encodedId == null) {
        encodedId = "";
    }
%>

<%-- <div class="bodyDiv"> --%>
<%-- 面包屑分级导航 --%>
<%@ include file="../fragment/breadcrumb.jsp" %>

<form name="pageForm"method="post" class="form-horizontal"
      action="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.jsonView, submitActionName+"/"+encodedId)%>">
    <div class="block-bg" style="padding-top: 24px; padding-bottom: 1px;">
        <%
        Map<String, String> model;
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
                    <%=I18n.getString((Arrays.stream(modelInfo.getGroupInfos())
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
                        boolean required = false;
                        if (!modelField.isEditable() && isEdit) {
                            readonly = "readonly";
                        }
                        if (idFieldName.equals(fieldName)) {
                            if (isEdit) {
                                readonly = "readonly";
                            }
                        }
                        if (PageBackendService.isFieldReadOnly(qzRequest, fieldName)) {
                            readonly = "readonly";
                        }

                        if (fieldName.equals(idFieldName)) {
                            required = true;
                        } else {
                            required = modelField.isRequired();
                        }

                        String valueFrom = /*modelField.valueFrom()*/"";
                        if (!"".equals(valueFrom)) {
                            valueFrom = "valueFrom='" + valueFrom.trim() + "'";
                        }

                        String fieldValue = String.valueOf(model.get(fieldName));// 需要在 isFieldReadOnly 之后，原因是 license 限制的 5 个并发会在其中被修改，总之最后读取值是最新的最准确的
                        List<String> fieldValues = fieldValue == null ? new ArrayList<>() : Arrays.asList(fieldValue.split(","));
                        if (fieldValue == null || fieldValue.equals("null")) {
                            fieldValue = "";
                        }
                %>
                <div class="form-group" id="form-item-<%=fieldName%>">
                    <label for="<%=fieldName%>" class="col-sm-4">
                        <%=required ? "<span  style=\"color:red;\">* </span>" : ""%>
                        <%=I18n.getString(menuAppName, "model.field." + qzRequest.getModel() + "." + fieldName)%>
                        <%
                            String fieldInfo = I18n.getString(menuAppName, "model.field.info." + qzRequest.getModel() + "." + fieldName);
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
                                    <textarea rows="3" disabled="disabled" name="<%=fieldName%>" <%=valueFrom%> class="form-control"
                                              readonly="readonly"><%=fieldValue%></textarea>
                                    <%
                                } else {
                                    %>
                                    <input type="text" name="<%=fieldName%>" value='<%=fieldValue%>' <%=valueFrom%>
                                           class="form-control" readonly="readonly">
                                    <%
                                }
                            } else {
                                FieldType fieldType = FieldType.valueOf(modelField.getType());
                                switch (fieldType) {
                                    case text:
                                    %>
                                    <input type="text" name="<%=fieldName%>" value='<%=fieldValue%>' <%=valueFrom%>
                                           class="form-control">
                                    <%
                                    break;
                                case number:
                                    %>
                                    <input type="number" min="<%=modelField.getMin()%>"
                                           max="<%=(modelField.isPort()?"65535":modelField.getMax())%>"
                                           name="<%=fieldName%>" value='<%=fieldValue%>' <%=valueFrom%> class="form-control">
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
                                    <textarea name="<%=fieldName%>" value='<%=fieldValue%>' <%=valueFrom%> class="form-control"
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
                                case groupmultiselect:
                                    %>
                                    <%@ include file="field_type/groupmultiselect.jsp" %>
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
                    boolean submitPermission = AccessControl.canAccess(menuAppName, qzRequest.getModel() + "/" + submitActionName, LoginManager.getLoginUser(session));
                    ModelActionInfo formCreateAction = modelInfo.getModelActionInfo(submitActionName);
                    if (submitPermission && (formCreateAction !=null && !formCreateAction.isDisable())) {
                %>
                <input type="submit" class="btn"
                       value='<%=I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + submitActionName)%>'>
                <%
                    }

                    boolean listPermission = AccessControl.canAccess(menuAppName, qzRequest.getModel() + "/" + Listable.ACTION_NAME_LIST, LoginManager.getLoginUser(session));
                    if (hasId && listPermission) {
                %>
                <a href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, Listable.ACTION_NAME_LIST)%>"
                   btn-type="goback" class="btn">
                    <%=PageBackendService.getMasterAppI18nString("page.cancel")%>
                </a>
                <%
                    }
                    boolean b2faPermission = AccessControl.canAccess(menuAppName, qzRequest.getModel() + "/" + ConsoleConstants.ACTION_NAME_2FA, LoginManager.getLoginUser(session));
                    if (b2faPermission) {
                %>
                <a href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.imageView, ConsoleConstants.ACTION_NAME_2FA)%>"
                    btn-type="qr2fa" class="btn">
                    <%=I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + ConsoleConstants.ACTION_NAME_2FA)%>
                </a>
                <%
                    }
                %>
            </div>
        </div>

        <div id="tempZone" style="display:none;"></div>
        <textarea name="pubkey" rows="3" disabled="disabled" style="display:none;">
        <%=AsymmetricDecryptor.getPublicKeyString()%>
        </textarea>

        <textarea name="eventConditions" rows="3" disabled="disabled" style="display:none;">
        <%
            // added by yuanwc for: ModelField 注解 show()
            StringBuilder conditionBuilder = new StringBuilder();
            conditionBuilder.append("{");
            Map<String, String> conditions = PageBackendService.modelFieldShowMap(qzRequest);
            for (Map.Entry<String, String> e : conditions.entrySet()) {
                //e.getValue().replace(/\&\&/g, '&').replace(/\|\|/g, '|');
                conditionBuilder.append("'").append(e.getKey()).append("' : '")
                        .append(e.getValue().replaceAll("\\&\\&", "&").replaceAll("\\|\\|", "|")).append("',");
            }
            if (conditionBuilder.indexOf(",") > 0) {
                conditionBuilder.deleteCharAt(conditionBuilder.lastIndexOf(","));
            }
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
