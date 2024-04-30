<%@ page import="qingzhou.registry.ModelFieldInfo" %>
<%@ page import="qingzhou.registry.ModelActionInfo" %>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    String contextPath = request.getContextPath();
    if (qzRequest == null || qzResponse == null) {
        return; // for 静态源码漏洞扫描
    }

    final boolean hasId = PageBackendService.hasIDField(qzRequest);
    LinkedHashMap<String, ModelFieldInfo> fieldInfos = new LinkedHashMap<>();
    String[] fieldNames = modelInfo.getFormFieldList();
    for (String fieldName : fieldNames) {
        fieldInfos.put(fieldName, modelInfo.getModelFieldInfo(fieldName));
    }
    List<Integer> indexToShow = new ArrayList<>();
    int num = -1;
    for (Map.Entry<String, ModelFieldInfo> e : fieldInfos.entrySet()) {
        num++;
        ModelFieldInfo modelField = e.getValue();
        /*if (!modelField.showToList()) {
            continue;
        }*/
        indexToShow.add(num);
    }

    int totalSize = qzResponse.getTotalSize();
    int pageNum = qzResponse.getPageNum();
    int pageSize = qzResponse.getPageSize();
%>

<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <div class="block-bg">
        <%
            if (!indexToShow.isEmpty()) {
        %>
        <%@ include file="../fragment/filter_form.jsp" %>
        <%
            }
        %>

        <hr style="margin-top: 4px;">

        <div class="table-tools tw-list-operate">
            <div class="tools-group">
                <%
                    // 用于判断是否需要操作列
                    boolean needOperationColumn = PageBackendService.needOperationColumn(qzRequest);
                    ModelActionInfo[] opsActions = PageBackendService.listCommonOps(qzRequest, qzResponse);
                    if (needOperationColumn) {
                        String modelIcon = modelInfo.getIcon();
                        for (ModelActionInfo action : opsActions) {
                            String actionKey = action.getCode();
                            String titleStr = I18n.getString(menuAppName, "model.action.info." + qzRequest.getModel() + "." + actionKey);
                            if (titleStr != null) {
                                titleStr = "data-tip='" + titleStr + "'";
                            } else {
                                titleStr = "data-tip='" + I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + actionKey) + "'";
                            }
                            boolean isAjaxAction = PageBackendService.isAjaxAction(actionKey);
                            String viewName = isAjaxAction ? ViewManager.jsonView : ViewManager.htmlView;
                %>
                <a id="<%=actionKey%>"
                   href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, viewName, actionKey)%>"
                   onclick='batchOps("<%=PageBackendService.buildRequestUrl(request, response, qzRequest, viewName, actionKey)%>","<%=actionKey%>")'
                        <%=titleStr%>
                   class="btn batch-ops" disabled="disabled" model-icon="<%=modelIcon%>" action-name="<%=actionKey%>"
                   data-name="" data-id=""
                        <%
                            if (isAjaxAction) {
                                out.print("act-ajax='true' act-confirm='" +
                                        String.format(PageBackendService.getMasterAppI18nString("page.operationConfirm"),
                                                I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + actionKey),
                                                I18n.getString(menuAppName, "model." + qzRequest.getModel())) + " ?' ");
                            }
                        %>
                >
                    <i class="icon icon-<%=action.getIcon()%>"></i>
                    <%=I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + actionKey)%>
                </a>
                <%
                        }
                    }
                %>
            </div>
        </div>

        <table class="table table-striped table-hover list-table responseScroll">
            <thead>
            <tr style="height:20px;">
                <%
                    if (opsActions.length > 0) {
                %>
                <th class="custom-checkbox">
                    <input type="checkbox" class="allcheck"/>
                </th>
                <%
                    }
                %>
                <th class="sequence"><%=PageBackendService.getMasterAppI18nString("page.list.order")%>
                </th>
                <%
                    for (Integer i : indexToShow) {
                        String name = PageBackendService.getFieldName(qzRequest, i);
                %>
                <th><%=I18n.getString(menuAppName, "model.field." + qzRequest.getModel() + "." + name)%>
                </th>
                <%
                    }
                    if (needOperationColumn) {
                        out.print("<th>" + PageBackendService.getMasterAppI18nString("page.action") + "</th>");
                    }
                %>
            </tr>
            </thead>
            <tbody>
            <%
                List<Map<String, String>> modelDataList = qzResponse.getDataList();
                if (modelDataList.isEmpty()) {
                    String dataEmpty = "<tr><td colspan='" + (indexToShow.size() + (needOperationColumn ? 2 : 1)) + "' align='center'>"
                            + "<img src='" + contextPath + "/static/images/data-empty.svg' style='width:160px; height: 160px;'><br>"
                            + "<span style='font-size:14px; font-weight:600; letter-spacing: 2px;'>" + PageBackendService.getMasterAppI18nString("page.none") + "</span></td>";
                    out.print(dataEmpty);
                } else {
                    int listOrder = (pageNum - 1) * pageSize;
                    for (int idx = 0; idx < modelDataList.size(); idx++) {
                        Map<String, String> modelBase = modelDataList.get(idx);
                        String modelIcon = modelInfo.getIcon();

                        String originUnEncodedId = modelBase.get(Listable.FIELD_NAME_ID);
                        String encodedId = PageBackendService.encodeId(originUnEncodedId);
            %>
            <tr>
                <%
                    if (opsActions.length > 0) {
                        boolean hasCheckAction = PageBackendService.listModelBaseOps(qzRequest, modelBase).length > 0;
                %>
                <td class="custom-checkbox">
                    <input type="checkbox"
                           value="<%= PageBackendService.encodeId(modelBase.get(Listable.FIELD_NAME_ID))%>"
                           name="<%=Listable.FIELD_NAME_ID%>" <%= hasCheckAction ? "class='morecheck'" : "disabled" %> />
                </td>
                <%
                    }
                %>
                <td class="sequence"><%=++listOrder%>
                </td>
                <%
                    ModelActionInfo targetAction = null;
                    if (AccessControl.canAccess(menuAppName, qzRequest.getModel() + "/" + Editable.ACTION_NAME_UPDATE, LoginManager.getLoginUser(session))
                            && AccessControl.canAccess(menuAppName, qzRequest.getModel() + "/" + Editable.ACTION_NAME_EDIT, LoginManager.getLoginUser(session))) {
                        targetAction = modelInfo.getModelActionInfo(Editable.ACTION_NAME_EDIT);
                    }
                    if (targetAction == null) {
                        if (AccessControl.canAccess(menuAppName, qzRequest.getModel() + "/" + Showable.ACTION_NAME_SHOW, LoginManager.getLoginUser(session))) {
                            targetAction = modelInfo.getModelActionInfo(Showable.ACTION_NAME_SHOW);
                        }
                    }
                    boolean isFirst = true;
                    for (Integer i : indexToShow) {
                        String value = modelBase.get(PageBackendService.getFieldName(qzRequest, i));
                        if (value == null) {
                            value = "";
                        }
                        if (isFirst && hasId && targetAction != null) {
                            isFirst = false;
                %>
                <td>
                    <a href='<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView , targetAction.getName() + "/" + encodedId)%>'
                       class="dataid tooltips"
                       record-action-id="<%=targetAction.getIcon()%>"
                       data-tip='<%=I18n.getString(menuAppName, "model.action.info." + qzRequest.getModel() + "." + targetAction.getName())%>'
                       data-tip-arrow="top"
                       style="color:#4C638F;">
                        <%=value%>
                    </a>
                </td>
                <%
                } else {
                    String fieldName = PageBackendService.getFieldName(qzRequest, i);
                    String linkField = /*modelInfo.getModelFieldInfo(fieldName).linkModel()*/null;
                    if (linkField != null && !linkField.isEmpty()) {
                        String[] split = linkField.split("\\.");
                        String idFieldValue = modelBase.get(Listable.FIELD_NAME_ID);
                %>
                <td>
                    <a href='<%=PageBackendService.encodeURL( response, ViewManager.htmlView + "/" + split[0] + "/" + split[1] + "?" + split[2] + "=" + idFieldValue)%>'
                       onclick='difModelActive("<%=qzRequest.getModel()%>","<%=split[0]%>")'
                       class="dataid tooltips" record-action-id="<%=split[1]%>"
                       data-tip='<%=I18n.getString(menuAppName, "model." + split[0])%>'
                       data-tip-arrow="top"
                       style="color:#4C638F;">
                        <%=value%>
                    </a>
                </td>
                <%
                } else {
                %>
                <td><%=value%>
                </td>
                <%
                            }
                        }
                    }
                    if (needOperationColumn) {
                %>
                <td>
                    <%
                        String[] actions = PageBackendService.getActionNamesShowToList(qzRequest);
                        for (String actionName : actions) {
                            ModelActionInfo action = modelInfo.getModelActionInfo(actionName);
                            if (action == null) {
                                continue;
                            }
                            if (PageBackendService.isActionEffective(qzRequest, modelBase, action) != null) {
                                continue;
                            }
                            String actionKey = action.getCode();
                            if (actionKey.equals(Editable.ACTION_NAME_EDIT)) {
                                continue;
                            }

                            if (!AccessControl.canAccess(menuAppName, qzRequest.getModel() + "/" + actionKey, LoginManager.getLoginUser(session))) {
                                continue;
                            }

                            String titleStr = I18n.getString(menuAppName, "model.action.info." + qzRequest.getModel() + "." + actionKey);
                            if (titleStr != null) {
                                titleStr = "data-tip='" + titleStr + "'";
                            } else {
                                titleStr = "data-tip='" + I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + actionKey) + "'";
                            }

                            boolean isAjaxAction = PageBackendService.isAjaxAction(actionName);
                            String viewName = isAjaxAction ? ViewManager.jsonView : ViewManager.htmlView;
                    %>
                    <a href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, viewName, actionKey + "/" + encodedId)%>" <%=titleStr%>
                       class="tw-action-link tooltips" data-tip-arrow="top"
                       model-icon="<%=modelIcon%>" action-name="<%=actionKey%>"
                       data-name="<%=originUnEncodedId%>" data-id="<%=(qzRequest.getModel() + "|" + encodedId)%>"
                            <%
                                if (actionKey.equals(Downloadable.ACTION_NAME_FILES)) {
                                    out.print(" downloadfile='" + PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.fileView, Downloadable.ACTION_NAME_DOWNLOAD + "/" + encodedId) + "'");
                                }
                                if (isAjaxAction) {
                                    out.print(" act-ajax='true' act-confirm='"
                                            + String.format(PageBackendService.getMasterAppI18nString("page.operationConfirm"),
                                            I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + actionKey),
                                            I18n.getString(menuAppName, "model." + qzRequest.getModel()))
                                            + " " + originUnEncodedId
                                            + "'");
                                }
                            %>
                    >
                        <i class="icon icon-<%=action.getIcon()%>"></i>
                        <%=I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + actionKey)%>
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

        <div style="text-align: center; <%=(totalSize < 1) ? "display:none;" : ""%>">
            <ul class="pager pager-loose" data-ride="pager" data-page="<%=pageNum%>"
                recPerPage="<%=pageSize%>"
                data-rec-total="<%=totalSize%>"
                partLinkUri="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, Listable.ACTION_NAME_LIST + "?markForAddCsrf")%>&<%=Listable.PARAMETER_PAGE_NUM%>="
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
                    params = params + $(this).attr("value") + ","
                }
            }
        });
        params = params.substring(0, params.length - 1);
        var str = url;
        if (str.indexOf("?") > -1) {
            url = str + "&<%=Listable.FIELD_NAME_ID%>=" + params;
        } else {
            url = str + "?<%=Listable.FIELD_NAME_ID%>=" + params;
        }
        $("#" + action, getRestrictedArea()).attr("href", url);
    }
</script>
