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
        if (!modelField.isList()) {
            continue;
        }
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
                    boolean canAccess = (SecurityFilter.canAccess(qzApp, qzModel + "/" + "add", LoginManager.getLoginUser(session)));
                    ModelActionInfo listCreateAction = modelInfo.getModelActionInfo("create");
                    ModelActionInfo listAddAction = modelInfo.getModelActionInfo("add");
                    if (canAccess && (listCreateAction != null) && (listAddAction != null)) {
                %>
                <a class="btn"
                   href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, "create")%>">
                    <i class="icon icon-plus-sign"></i>
                    <%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + "create")%>
                </a>
                <%
                    }

                    // 用于判断是否需要操作列
                    boolean needOperationColumn = PageBackendService.needOperationColumn(qzRequest);
                    ModelActionInfo[] opsActions = PageBackendService.listCommonOps(qzRequest, qzResponse);
                    if (needOperationColumn) {
                        String modelIcon = modelInfo.getIcon();
                        for (ModelActionInfo action : opsActions) {
                            String actionKey = action.getCode();
                            String titleStr = I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionKey);
                            if (titleStr != null) {
                                titleStr = "data-tip='" + titleStr + "'";
                            } else {
                                titleStr = "data-tip='" + I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey) + "'";
                            }
                            boolean isAjaxAction = action.isAjax();
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
                                        String.format(I18n.getKeyI18n("page.operationConfirm"),
                                                I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey),
                                                I18n.getModelI18n(qzApp, "model." + qzModel)) + " ?' ");
                            }
                        %>
                >
                    <i class="icon icon-<%=action.getIcon()%>"></i>
                    <%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey)%>
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
                <th class="sequence"><%=I18n.getKeyI18n("page.list.order")%>
                </th>
                <%
                    for (Integer i : indexToShow) {
                        String name = PageBackendService.getFieldName(qzRequest, i);
                %>
                <th><%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + name)%>
                </th>
                <%
                    }
                    if (needOperationColumn) {
                        out.print("<th>" + I18n.getKeyI18n("page.action") + "</th>");
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
                            + "<span style='font-size:14px; font-weight:600; letter-spacing: 2px;'>" + I18n.getKeyI18n("page.none") + "</span></td>";
                    out.print(dataEmpty);
                } else {
                    int listOrder = (pageNum - 1) * pageSize;
                    for (int idx = 0; idx < modelDataList.size(); idx++) {
                        Map<String, String> modelBase = modelDataList.get(idx);
                        String modelIcon = modelInfo.getIcon();

                        String originUnEncodedId = modelBase.get(idFieldName);
                        String encodedId = PageBackendService.encodeId(originUnEncodedId);
            %>
            <tr>
                <%
                    if (opsActions.length > 0) {
                        boolean hasCheckAction = PageBackendService.listModelBaseOps(qzRequest, modelBase).length > 0;
                %>
                <td class="custom-checkbox">
                    <input type="checkbox"
                           value="<%= PageBackendService.encodeId(modelBase.get(idFieldName))%>"
                           name="<%=idFieldName%>" <%= hasCheckAction ? "class='morecheck'" : "disabled" %> />
                </td>
                <%
                    }
                %>
                <td class="sequence"><%=++listOrder%>
                </td>
                <%
                    ModelActionInfo targetAction = modelInfo.getModelActionInfo("show");
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
                    <a href='<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView , targetAction.getCode() + "/" + encodedId)%>'
                       class="dataid tooltips"
                       record-action-id="<%=targetAction.getCode()%>"
                       data-tip='<%=I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + targetAction.getCode())%>'
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
                        String idFieldValue = modelBase.get(idFieldName);
                %>
                <td>
                    <a href='<%=PageBackendService.encodeURL( response, ViewManager.htmlView + "/" + split[0] + "/" + split[1] + "?" + split[2] + "=" + idFieldValue)%>'
                       onclick='difModelActive("<%=qzModel%>","<%=split[0]%>")'
                       class="dataid tooltips" record-action-id="<%=split[1]%>"
                       data-tip='<%=I18n.getModelI18n(qzApp, "model." + split[0])%>'
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
                            if (SecurityFilter.isActionAvailable(qzApp, modelBase, action) != null) {
                                continue;
                            }
                            String actionKey = action.getCode();

                            if (!SecurityFilter.canAccess(qzApp, qzModel + "/" + actionKey, LoginManager.getLoginUser(session))) {
                                continue;
                            }

                            String titleStr = I18n.getModelI18n(qzApp, "model.action.info." + qzModel + "." + actionKey);
                            if (titleStr != null) {
                                titleStr = "data-tip='" + titleStr + "'";
                            } else {
                                titleStr = "data-tip='" + I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey) + "'";
                            }

                            boolean isAjaxAction = action.isAjax();
                            String viewName = isAjaxAction ? ViewManager.jsonView : ViewManager.htmlView;
                    %>
                    <a href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, viewName, actionKey + "/" + encodedId)%>" <%=titleStr%>
                       class="tw-action-link tooltips" data-tip-arrow="top"
                       model-icon="<%=modelIcon%>" action-name="<%=actionKey%>"
                       data-name="<%=originUnEncodedId%>" data-id="<%=(qzModel + "|" + encodedId)%>"
                            <%
                                if (actionKey.equals("files")) {
                                    out.print(" downloadfile='" + PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.fileView, "download/" + encodedId) + "'");
                                }
                                if (isAjaxAction) {
                                    out.print(" act-ajax='true' act-confirm='"
                                            + String.format(I18n.getKeyI18n("page.operationConfirm"),
                                            I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey),
                                            I18n.getModelI18n(qzApp, "model." + qzModel))
                                            + " " + originUnEncodedId
                                            + "'");
                                }
                            %>
                    >
                        <i class="icon icon-<%=action.getIcon()%>"></i>
                        <%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + actionKey)%>
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
                partLinkUri="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, "list" + "?markForAddCsrf")%>&<%="pageNum"%>="
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
            url = str + "&<%=idFieldName%>=" + params;
        } else {
            url = str + "?<%=idFieldName%>=" + params;
        }
        $("#" + action, getRestrictedArea()).attr("href", url);
    }
</script>
