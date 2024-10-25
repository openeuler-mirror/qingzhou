<%@ page pageEncoding="UTF-8" %>

<%@ include file="../fragment/head.jsp" %>

<%
    String contextPath = request.getContextPath();
    String idField = modelInfo.getIdField();
    ModelFieldInfo idFieldFieldInfo = modelInfo.getModelFieldInfo(idField);

    String[] listFields = modelInfo.getFieldsToList();
    if (!modelInfo.isShowIdField()) {
        List<String> showListFields = new ArrayList<>();
        for (String listField : listFields) {
            if (!listField.equals(idField)) {
                showListFields.add(listField);
            }
        }
        listFields = showListFields.toArray(new String[0]);
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
    %>
    <%@ include file="../fragment/filter_form.jsp" %>
    <%
        if (fieldsToListSearch.length > 0) {
    %>
    <hr style="margin-top: 4px;">
    <%
        }
    %>

    <div class="table-tools tw-list-operate">
        <div class="tools-group">
            <%
                for (String actionName : headActions) {
                    ModelActionInfo action = modelInfo.getModelActionInfo(actionName);

                    String customActionId = "";
                    if (action.getLinkFields() != null && action.getLinkFields().length > 0) {
                        customActionId = " custom-action-id='popup-" + qzApp + "-" + qzModel + "-" + action.getCode() + "'";
                    }

            %>
            <a class="btn" data-tip-arrow="top" action-name="<%=actionName%>" <%=customActionId%>
               data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionName)%>'
               href="<%=PageUtil.buildRequestUrl(request, response, qzRequest, Utils.notBlank(customActionId)?JsonView.FLAG:HtmlView.FLAG, actionName)%>"
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
                for (String actionKey : batchActions) {
                    ModelActionInfo actionInfo = modelInfo.getModelActionInfo(actionKey);

                    String operationConfirm = String.format(I18n.getKeyI18n("page.operationConfirm"),
                            I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey),
                            I18n.getModelI18n(qzApp, "model." + qzModel));

                    String actionUrl = PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG, actionKey);
            %>
            <a id="<%=actionKey%>" action-name="<%=actionKey%>"
               href="<%=actionUrl%>"
               onclick='batchOps("<%=actionUrl%>","<%=actionKey%>","<%=idField%>","<%=idFieldFieldInfo.getSeparator()%>")'
               data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionKey)%>'
               class="btn batch-ops"
               disabled="disabled" model-icon="<%=modelInfo.getIcon()%>"
               data-name="" data-id="" act-ajax='true' act-confirm='<%=operationConfirm%> ?'>
                <i class="icon icon-<%=actionInfo.getIcon()%>"></i>
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
                for (String field : listFields) {
                    String ifHideStyle = "";
                    if (field.equals(idField)) {
                        ifHideStyle = "; display: none";
                        if (modelInfo.isShowIdField()) {
                            ifHideStyle = "";
                        }
                    }
                    ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
                    int width;
                    if (fieldInfo.getWidthPercent() > 0) {
                        width = fieldInfo.getWidthPercent();
                    } else {
                        width = 100 / (listFields.length + otherTh);
                    }
            %>
            <th style="width: <%=width%>% <%=ifHideStyle%>"><%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + field)%>
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
                String dataEmpty = "<tr><td colspan='" + (((batchActions.length > 0) ? 1 : 0) + (modelInfo.isShowOrderNumber() ? 1 : 0) + listFields.length + (listActions.length > 0 ? 1 : 0)) + "' align='center'>"
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
                if (modelInfo.isShowOrderNumber()) {
            %>
            <td class="sequence"><%=++listOrder%>
            </td>
            <%
                }
                for (String field : listFields) {
                    String hideStyle = "";
                    if (field.equals(idField)) {
                        hideStyle = "display: none";
                        if (modelInfo.isShowIdField()) {
                            hideStyle = "";
                        }
                    }

                    ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
                    String value = modelData.get(field);
                    if (value == null) {
                        value = "";
                    }
                    //兼容表单组件 例：bool.jsp中使用的是fieldValue和fieldName
                    String fieldValue = value;
                    java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(fieldInfo.getSeparator()));
                    String fieldName = field;
                    String echoGroup = "";
            %>
            <td style="<%=hideStyle%>">
                <%
                    if ((field.equals(idField)) || fieldInfo.isLinkShow()) {
                        boolean hasShowAction = false;
                        if (SecurityController.isActionPermitted(qzApp, qzModel, Show.ACTION_SHOW, currentUser)) {
                            hasShowAction = true;
                            String condition = modelInfo.getModelActionInfo(Show.ACTION_SHOW).getShow();
                            if (Utils.notBlank(condition)) {
                                hasShowAction = SecurityController.checkRule(condition, modelData::get);
                            }
                        }
                        if (hasShowAction) {
                %>

                <a href='<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG , Show.ACTION_SHOW + "/" + encodedItemId)%>'
                   class="dataid tooltips"
                   record-action-id="<%=Show.ACTION_SHOW%>"
                   style="color:#4C638F;">
                    <%=PageUtil.styleFieldValue(value, fieldInfo, modelInfo)%>
                </a>
                <%
                    } else {
                        out.print(PageUtil.styleFieldValue(value, fieldInfo, modelInfo));
                    }
                } else if (fieldInfo.isUpdate()) {
                    switch (fieldInfo.getInputType()){
                        case bool:
                %>
                <%@ include file="../fragment/field_type/bool.jsp" %>
                <%
                            break;
                        case select:
                %>
                <%@ include file="../fragment/field_type/select.jsp" %>
                <%
                            break;
                        case multiselect:
                %>
                <%@ include file="../fragment/field_type/multiselect.jsp" %>
                <%
                            break;
                        default:
                %>
                <%
                            out.print("<div class=\"input-class\"> <input type=\"text\" name=\""+ fieldName +"\" value=\""+ fieldValue +"\" class=\"form-control\"></div>");
                            break;
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
                        refValue = value.replace(fieldInfo.getSeparator(), refFieldInfo.getSeparator());
                %>
                <a href='<%=PageUtil.buildCustomUrl(request, response, qzRequest,HtmlView.FLAG, refModelName, qingzhou.api.type.List .ACTION_LIST + "?" + refFieldName + "=" + refValue)%>'
                   onclick='difModelActive("<%=qzRequest.getModel()%>","<%=refModelName%>")'
                   class="dataid tooltips" record-action-id="<%=qingzhou.api.type.List.ACTION_LIST%>"
                   data-tip='<%=I18n.getModelI18n(qzApp, "model." + refModelName)%>' data-tip-arrow="top"
                   style="color:#4C638F;">
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
                            showAction = SecurityController.checkRule(action.getShow(), modelData::get);
                        }
                        if (!showAction) continue;

                        String customActionId = "";
                        if (action.getLinkFields() != null && action.getLinkFields().length > 0) {
                            customActionId = " custom-action-id='popup-" + qzApp + "-" + qzModel + "-" + action.getCode() + "-" + encodedItemId + "'";
                        }

                        boolean useJsonUri = Utils.notBlank(customActionId)
                                || actionName.equals(Delete.ACTION_DELETE)
                                || actionName.equals(Download.ACTION_FILES);
                %>
                <a href="<%=PageUtil.buildRequestUrl(request, response, qzRequest,
                            useJsonUri ? JsonView.FLAG : HtmlView.FLAG,
                            actionName + "/" + encodedItemId)%>"
                   data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionName)%>'
                   class="tw-action-link tooltips" data-tip-arrow="top"
                   model-icon="<%=modelInfo.getIcon()%>" action-name="<%=actionName%>"
                   data-name="<%=originUnEncodedId%>" data-id="<%=(qzModel + "|" + encodedItemId)%>"
                        <%
                            if (actionName.equals(Download.ACTION_FILES)) {
                                out.print(" downloadfile='" + PageUtil.buildRequestUrl(request, response, qzRequest, StreamView.FLAG, Download.ACTION_DOWNLOAD + "/" + encodedItemId) + "'");
                            }

                            if (Utils.notBlank(customActionId)) {
                                out.print(customActionId);
                            }

                            if (useJsonUri && Utils.isBlank(customActionId)) {
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
</script>
