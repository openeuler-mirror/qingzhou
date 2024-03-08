<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    if (qzRequest == null || qzResponse == null || modelManager == null) {
        return; // for 静态源码漏洞扫描
    }

    boolean isEdit = Objects.equals(Editable.ACTION_NAME_EDIT, qzRequest.getActionName());
    String submitActionName = PageBackendService.getSubmitActionName(qzRequest);
    String idFieldName = Listable.FIELD_NAME_ID;
    ModelFieldData idField = modelManager.getModelField(qzRequest.getModelName(), idFieldName);
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

<form name="pageForm"
      action="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.jsonView, submitActionName+"/"+encodedId)%>"
      method="post" class="form-horizontal">
    <div class="block-bg" style="padding-top: 24px; padding-bottom: 1px;">
            <%
        Map<String, String> model;
        List<Map<String, String>> models = qzResponse.getDataList();
        if (!models.isEmpty()) {
            model = models.get(0);
            Map<String, Map<String, ModelFieldData>> fieldMapWithGroup = PageBackendService.getGroupedModelFieldMap(qzRequest);
            Set<String> groups = fieldMapWithGroup.keySet();
            long suffixId = System.currentTimeMillis();
            if (groups.size() > 1) {
                %>
        <ul class="nav nav-tabs">
            <%
                boolean isFirst = true;
                for (String group : groups) {
                    group = "".equals(group) ? "OTHERS" : group;
            %>
            <li <%=isFirst ? "class='active'" : ""%>>
                <a data-tab href="#group-<%=group%>-<%=suffixId%>"
                   tabGroup="<%=group%>">
                    <%=I18n.getString(modelManager.getGroup(qzRequest.getModelName(), group).i18n())%>
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
                    Map<String, ModelFieldData> groupFieldMap = fieldMapWithGroup.get(group);
                    if (groupFieldMap == null) {
                        continue;
                    }
                    for (Map.Entry<String, ModelFieldData> e : groupFieldMap.entrySet()) {
                        ModelFieldData modelField = e.getValue();
                        if (modelField.disableOnCreate() && !isEdit) {
                            continue;
                        }
                        if (!modelField.showToEdit() && isEdit) {
                            continue;
                        }

                        String fieldName = e.getKey();

                        if (modelField.clientEncrypt()) {
                            passwordFields.add(fieldName);
                        }

                        String readonly = "";
                        boolean required;
                        if (modelField.disableOnEdit() && isEdit) {
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
                            required = modelField.required();
                        }

                        String valueFrom = modelField.valueFrom();
                        if (!"".equals(valueFrom)) {
                            valueFrom = "valueFrom='" + valueFrom.trim() + "'";
                        }

                        String fieldValue = model.get(fieldName);// 需要在 isFieldReadOnly 之后，原因是 license 限制的 5 个并发会在其中被修改，总之最后读取值是最新的最准确的
                        List<String> fieldValues = fieldValue == null ? new ArrayList<>() : Arrays.asList(fieldValue.split(ConsoleConstants.DATA_SEPARATOR));
                        if (fieldValue == null) {
                            fieldValue = "";
                        }

                %>
                <div class="form-group" id="form-item-<%=fieldName%>">
                    <label for="<%=fieldName%>" class="col-sm-4">
                        <%=required ? "<span  style=\"color:red;\">* </span>" : ""%>
                        <%=I18n.getString(menuAppName, "model.field." + qzRequest.getModelName() + "." + fieldName)%>
                        <%
                            String fieldInfo = I18n.getString(menuAppName, "model.field.info." + qzRequest.getModelName() + "." + fieldName);
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
                            if (StringUtil.notBlank(readonly)) {
                                if (FieldType.textarea.equals(modelField.type())) {
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
                            switch (modelField.type()) {
                                case text:
                        %>
                        <input type="text" name="<%=fieldName%>" value='<%=fieldValue%>' <%=valueFrom%>
                               class="form-control">
                        <%
                                break;
                            case number:
                        %>
                        <input type="number" min="<%=modelField.min()%>"
                               max="<%=(modelField.isPort()?"65535":modelField.max())%>"
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
                            case selectCharset:
                        %>
                        <%@ include file="field_type/select.jsp" %>
                        <%
                                break;
                            case multiselect:
                        %>
                        <%@ include file="field_type/multiselect.jsp" %>
                        <%
                                break;
                            case groupedMultiselect:
                        %>
                        <%@ include file="field_type/groupedMultiselect.jsp" %>
                        <%
                                break;
                            case checkbox:
                        %>
                        <%@ include file="field_type/checkbox.jsp" %>
                        <%
                                break;
                            case sortableCheckbox:
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
                                        throw new IllegalStateException(modelField.type().name() + ".jsp not found.");
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
                    boolean submitPermission = AccessControl.canAccess(menuAppName, qzRequest.getModelName() + "/" + submitActionName, LoginManager.getLoginUser(session));
                    if (submitPermission) {
                %>
                <input type="submit" class="btn"
                       value='<%=I18n.getString(menuAppName, "model.action." + qzRequest.getModelName() + "." + submitActionName)%>'>
                <%
                    }

                    boolean listPermission = AccessControl.canAccess(menuAppName, qzRequest.getModelName() + "/" + Listable.ACTION_NAME_LIST, LoginManager.getLoginUser(session));
                    if (hasId && listPermission) {
                %>
                <a href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, Listable.ACTION_NAME_LIST)%>"
                   btn-type="goback" class="btn">
                    <%=PageBackendService.getMasterAppI18nString("page.cancel")%>
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
            // added by yuanwc for: ModelField 注解 effectiveWhen()
            StringBuilder conditionBuilder = new StringBuilder();
            conditionBuilder.append("{");
            Map<String, String> conditions = PageBackendService.modelFieldEffectiveWhenMap(qzRequest);
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
</form>
<%-- </div> --%>
