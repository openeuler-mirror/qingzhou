<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    String contextPath = request.getContextPath();
    if (qzRequest == null || qzResponse == null) {
        return; // for 静态源码漏洞扫描
    }

    final boolean hasId = ConsoleUtil.hasIDField(qzRequest);

    LinkedHashMap<String, ModelField> fieldInfos = ConsoleUtil.getModelFieldMap(qzRequest);
    List<Integer> indexToShow = new ArrayList<>();
    int num = -1;
    for (Map.Entry<String, ModelField> e : fieldInfos.entrySet()) {
        num++;
        ModelField modelField = e.getValue();
        if (!modelField.showToList()) {
            continue;
        }
        indexToShow.add(num);
    }

    int totalSize = qzResponse.modelData().getTotalSize();
    int pageNum = qzResponse.modelData().getPageNum();
    int pageSize = qzResponse.modelData().getPageSize();
%>

<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <div class="block-bg">
        <form name="filterForm" id="filterForm" method="POST"
              action="<%=ConsoleUtil.encodeURL(request, response, ViewManager.htmlView + "/" + qzRequest.getTargetType() + "/" + qzRequest.getTargetName() + "/" + qzRequest.getModelName() + "/" + ListModel.ACTION_NAME_LIST)%>">
            <div class="row filterForm" style="margin-top: 10px;">
                <%
                for (Integer i : indexToShow) {
                    String fieldName = modelManager.getFieldName(qzRequest.getModelName(), i);
                    List<Option> modelOptionsEntry = null;
                    if (ConsoleUtil.isFilterSelect(qzRequest, i)) {
                        try {
                            OptionManager modelOptions = ConsoleUtil.fieldOptions(qzRequest, fieldName);
                            if (modelOptions != null) {
                                modelOptionsEntry = modelOptions.options();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    %>
                    <div class='col-md-3 list-page-padding-bottom <%=modelOptionsEntry != null ? "listPageFilterSelect" : "" %>'>
                        <div class="input-control has-label-left ">
                            <%
                                if (modelOptionsEntry != null) {
                            %>
                            <%@ include file="../fragment/filter_select.jsp" %>
                            <%
                            } else {
                                String showHtml = (request.getParameter(fieldName) == null) ? "" : request.getParameter(fieldName);
                                if (StringUtil.notBlank(showHtml)) {
                                    if (SafeCheckerUtil.checkIsXSS(showHtml)) {
                                        showHtml = "";
                                    }
                                }
                            %>
                            <input id="<%=fieldName%>" type="text" name="<%=fieldName%>"
                                   value='<%=showHtml%>'
                                   class="form-control" placeholder="">
                            <%
                                }
                            %>
                            <label for="<%=fieldName%>"
                                   class="input-control-label-left"><%=I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.field." + qzRequest.getModelName() + "." + fieldName)%>
                            </label>
                        </div>
                    </div>
                    <%
                }
                %>
                <div class="col-md-3 search-btn" style="margin-bottom: 16px;">
                    <span class="input-group-btn col-md-4" style="width: 18%;padding-left:0px;">
                        <a class="btn"
                           href="<%=ConsoleUtil.buildRequestUrl(request, response, qzRequest,ViewManager.htmlView,ListModel.ACTION_NAME_LIST)%>"
                           form="filterForm">
                            <i class="icon icon-search"></i> <%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.filter")%>
                        </a>
                    </span>
                </div>
            </div>
        </form>

        <hr style="margin-top: 4px;">

        <div class="table-tools tw-list-operate">
            <div class="tools-group">
                <%
                    String errorOfActionEffective = modelManager.getModelInstance(qzRequest.getModelName()).actionNotEffective(qzRequest);
                    boolean canAccess = (AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(),qzRequest.getModelName() + "/" + AddModel.ACTION_NAME_ADD, LoginManager.getLoginUser(session)));
                    ModelAction listCreateAction = modelManager.getModelAction(qzRequest.getModelName(), AddModel.ACTION_NAME_CREATE);
                    ModelAction listAddAction = modelManager.getModelAction(qzRequest.getModelName(), AddModel.ACTION_NAME_ADD);
                    if (canAccess && listCreateAction != null && listAddAction != null && errorOfActionEffective == null) {
                        %>
                        <a class="btn"
                           href="<%=ConsoleUtil.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, AddModel.ACTION_NAME_CREATE)%>">
                            <i class="icon icon-<%=listCreateAction.icon()%>"></i>
                            <%=I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action." + qzRequest.getModelName() + "." + AddModel.ACTION_NAME_CREATE)%>
                        </a>
                        <%
                    }

                    boolean downloadPermission = (AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(),qzRequest.getModelName() + "/" + DownloadModel.ACTION_NAME_DOWNLOADFILE, LoginManager.getLoginUser(session))
                            && AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(),qzRequest.getModelName() + "/" + DownloadModel.ACTION_NAME_DOWNLOADLIST, LoginManager.getLoginUser(session)));
                    final ModelAction downloadListModelAction = modelManager.getModelAction(qzRequest.getModelName(), DownloadModel.ACTION_NAME_DOWNLOADLIST);
                    if (downloadListModelAction != null && downloadPermission && ConsoleUtil.actionShowToListHead(qzRequest, downloadListModelAction.name())) {
                        %>
                        <a style="margin-left:6px" class="btn" btn-type="<%=DownloadModel.ACTION_NAME_DOWNLOADLIST%>"
                           action-name="<%=DownloadModel.ACTION_NAME_DOWNLOADLIST%>"
                           href="<%= ConsoleUtil.isDisableDownload() ? "javascript:void(0);" : ConsoleUtil.buildRequestUrl(request, response, qzRequest, ViewManager.jsonView, DownloadModel.ACTION_NAME_DOWNLOADLIST)%>"
                                <%
                                out.print(ConsoleUtil.isDisableDownload() ? " disabled ":"" + " downloadfile='" + ConsoleUtil.buildRequestUrl(request, response, qzRequest, ViewManager.fileView, DownloadModel.ACTION_NAME_DOWNLOADFILE) + "' ");%>
                                out.print("act-ajax='true' act-confirm='" +
                                        String.format(I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.operationConfirm"),
                                                I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action." + qzRequest.getModelName() + "." + DownloadModel.ACTION_NAME_DOWNLOADLIST),
                                                I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model." + qzRequest.getModelName()))
                                        + " ?' ");
                                %>

                            <i class="icon icon-<%=downloadListModelAction.icon()%>"></i>
                            <%=I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action." + qzRequest.getModelName() + "." + DownloadModel.ACTION_NAME_DOWNLOADLIST)%>
                        </a>
                        <%
                    }

                    // 用于判断是否需要操作列
                    boolean needOperationColumn = ConsoleUtil.needOperationColumn(qzRequest, qzResponse, session);
                    ModelAction[] opsActions = ConsoleUtil.listCommonOps(qzRequest, qzResponse, session);
                    if (needOperationColumn) {
                        String modelIcon = modelManager.getModel(qzRequest.getModelName()).icon();
                        for (ModelAction action : opsActions) {
                            String actionKey = action.name();
                            String titleStr = I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action.info." + qzRequest.getModelName() + "." + actionKey);
                            if (StringUtil.notBlank(titleStr)) {
                                titleStr = "data-tip='" + titleStr + "'";
                            } else {
                                titleStr = "data-tip='" + I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action." + qzRequest.getModelName() + "." + actionKey) + "'";
                            }
                            boolean isAjaxAction = ConsoleUtil.actionsWithAjax(qzRequest, actionKey);
                            String viewName = isAjaxAction ? ViewManager.jsonView : ViewManager.htmlView;
                            %>
                            <a id="<%=actionKey%>"
                               href="<%=ConsoleUtil.buildRequestUrl(request, response, qzRequest, viewName, actionKey)%>"
                               onclick='batchOps("<%=ConsoleUtil.buildRequestUrl(request, response, qzRequest, viewName, actionKey)%>","<%=actionKey%>")'
                                    <%=titleStr%>
                               class="btn batch-ops"
                               disabled="disabled"
                               model-icon="<%=modelIcon%>" action-name="<%=actionKey%>"
                               data-name="" data-id=""
                                    <%
                                        if (isAjaxAction) {
                                            out.print("act-ajax='true' act-confirm='" +
                                                    String.format(I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.operationConfirm"),
                                                    I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action." + qzRequest.getModelName() + "." + actionKey),
                                                    I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model." + qzRequest.getModelName())) + " ?' ");
                                        }
                                    %>
                            >
                                <i class="icon icon-<%=action.icon()%>"></i>
                                <%=I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action." + qzRequest.getModelName() + "." + actionKey)%>
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
                    <th>
                        <input type="checkbox" class="allcheck"/>
                    </th>
                    <%
                }
                %>
                <th><%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.list.order")%>
                </th>
                <%
                for (Integer i : indexToShow) {
                    String name = modelManager.getFieldName(qzRequest.getModelName(), i);
                    %>
                        <th><%=I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.field." + qzRequest.getModelName() + "." + name)%></th>
                        <%
                }
                if (needOperationColumn) {
                    out.print("<th>" + I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.action") + "</th>");
                }
                %>
            </tr>
            </thead>
            <tbody>
            <%
                List<Map<String, String>> modelDataList = qzResponse.modelData().getDataList();
                if (modelDataList.isEmpty()) {
                    String dataEmpty = "<tr><td colspan='" + (indexToShow.size() + (needOperationColumn ? 2 : 1)) + "' align='center'>"
                            + "<img src='" + contextPath + "/static/images/data-empty.svg' style='width:160px; height: 160px;'><br>"
                            + "<span style='font-size:14px; font-weight:600; letter-spacing: 2px;'>" + I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.none") + "</span></td>";
                    out.print(dataEmpty);
                } else {
                    int listOrder = (pageNum - 1) * pageSize;
                    for (int idx = 0; idx < modelDataList.size(); idx++) {
                        String originUnEncodedId = null;
                        String encodedId = null;
                        Map<String, String> modelBase = modelDataList.get(idx);
                        String modelIcon = modelManager.getModel(qzRequest.getModelName()).icon();
                        if (hasId) {
                            originUnEncodedId = modelBase.get(ListModel.FIELD_NAME_ID);
                            encodedId = originUnEncodedId;
                            if (ConsoleSDK.needEncode(originUnEncodedId)) {
                                encodedId = ConsoleSDK.encodeId(originUnEncodedId);
                            }
                        }
                        %>
                        <tr>
                            <%
                            if (hasId) {
                                String idValue = modelBase.get(ListModel.FIELD_NAME_ID);
                                if (opsActions.length > 0) {
                                    boolean hasCheckAction = ConsoleUtil.listModelBaseOps(qzRequest, qzResponse, session, modelBase).length > 0;
                                    %>
                                    <td>
                                        <input type="checkbox"
                                               value="<%= ConsoleSDK.needEncode(idValue) ?  ConsoleSDK.encodeId(idValue) : idValue%>"
                                               name="<%=ListModel.FIELD_NAME_ID%>" <%= hasCheckAction ? "class='morecheck'" : "disabled" %> />
                                    </td>
                                    <%
                                }
                            }
                            %>
                            <td><%=++listOrder%>
                            </td>
                            <%
                            ModelAction targetAction = null;
                            if (AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(),qzRequest.getModelName() + "/" + EditModel.ACTION_NAME_UPDATE, LoginManager.getLoginUser(session))
                                    && AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(),qzRequest.getModelName() + "/" + EditModel.ACTION_NAME_EDIT, LoginManager.getLoginUser(session))) {
                                targetAction = modelManager.getModelAction(qzRequest.getModelName(), EditModel.ACTION_NAME_EDIT);
                            }
                            if (targetAction == null) {
                                if (AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(),qzRequest.getModelName() + "/" + ShowModel.ACTION_NAME_SHOW, LoginManager.getLoginUser(session))) {
                                    targetAction = modelManager.getModelAction(qzRequest.getModelName(), ShowModel.ACTION_NAME_SHOW);
                                }
                            }
                            boolean isFirst = true;
                            for (Integer i : indexToShow) {
                                String value = modelBase.get(modelManager.getFieldName(qzRequest.getModelName(), i));
                                if (value == null) {
                                    value = "";
                                }
                                ModelAction actionEdit = modelManager.getModelAction(qzRequest.getModelName(), EditModel.ACTION_NAME_EDIT);
                                if (isFirst && hasId && targetAction != null && actionEdit != null && actionEdit.effectiveWhen() != "") {
                                    isFirst = false;
                                    %>
                                    <td>
                                        <a href='<%=ConsoleUtil.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView , targetAction.name() + "/" + encodedId)%>'
                                           class="dataid tooltips"
                                           record-action-id="<%=targetAction.name()%>"
                                           data-tip='<%=I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action.info." + qzRequest.getModelName() + "." + targetAction.name())%>'
                                           data-tip-arrow="top"
                                           style="color:#4C638F;">
                                            <%=value%>
                                        </a>
                                    </td>
                                    <%
                                } else {
                                    String fieldName = modelManager.getFieldName(qzRequest.getModelName(), i);
                                    String linkField = modelManager.getModelField(qzRequest.getModelName(), fieldName).linkModel();
                                    if (StringUtil.notBlank(linkField)) {
                                        String[] split = linkField.split("\\.");
                                        String idFieldValue = modelBase.get(ListModel.FIELD_NAME_ID);
                                        %>
                                        <td>
                                            <a href='<%=ConsoleUtil.encodeURL(request, response, ViewManager.htmlView + "/" + qzRequest.getTargetType() + "/" + qzRequest.getTargetName() + "/" + split[0] + "/" + split[1] + "?" + split[2] + "=" + idFieldValue)%>'
                                               onclick='difModelActive("<%=qzRequest.getModelName()%>","<%=split[0]%>")'
                                               class="dataid tooltips" record-action-id="<%=split[1]%>"
                                               data-tip='<%=I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model." + split[0])%>'
                                               data-tip-arrow="top"
                                               style="color:#4C638F;">
                                                <%=value%>
                                            </a>
                                        </td>
                                        <%
                                    } else {
                                        %>
                                        <td><%=value%></td>
                                        <%
                                    }
                                }
                            }
                            %>

                            <%
                            ModelAction[] actions = ConsoleUtil.getShowToListActions(qzRequest);
                            %>
                            <td>
                            <%
                            for (ModelAction action : actions) {
                                if(action == null){
                                    continue;
                                }
                                if (ConsoleUtil.isActionEffective(qzRequest, modelBase, action) != null) {
                                    continue;
                                }
                                String actionKey = action.name();
                                if (actionKey.equals(EditModel.ACTION_NAME_EDIT)) {
                                    continue;
                                }

                                if (!AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(), qzRequest.getModelName() + "/" + actionKey, LoginManager.getLoginUser(session))) {
                                    continue;
                                }

                                String titleStr = I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action.info." + qzRequest.getModelName() + "." + actionKey);
                                if (StringUtil.notBlank(titleStr)) {
                                    titleStr = "data-tip='" + titleStr + "'";
                                } else {
                                    titleStr = "data-tip='" + I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action." + qzRequest.getModelName() + "." + actionKey) + "'";
                                }

                                boolean isAjaxAction = ConsoleUtil.actionsWithAjax(qzRequest, actionKey);
                                String viewName = isAjaxAction ? ViewManager.jsonView : ViewManager.htmlView;
                                %>
                                <a href="<%=actionKey.equals(DownloadModel.ACTION_NAME_DOWNLOADLIST) && ConsoleUtil.isDisableDownload() ? "javascript:void(0);" : ConsoleUtil.buildRequestUrl(request, response, qzRequest, viewName, actionKey + "/" + encodedId)%>" <%=titleStr%>
                                   class="tw-action-link tooltips" data-tip-arrow="top"
                                   model-icon="<%=modelIcon%>" action-name="<%=actionKey%>"
                                   data-name="<%=originUnEncodedId%>" data-id="<%=(qzRequest.getModelName() + "|" + encodedId)%>"
                                    <%
                                    if (actionKey.equals(DownloadModel.ACTION_NAME_DOWNLOADLIST)) {
                                        out.print(ConsoleUtil.isDisableDownload() ? " disabled ":"" + "downloadfile='" + ConsoleUtil.buildRequestUrl(request, response, qzRequest, ViewManager.fileView, DownloadModel.ACTION_NAME_DOWNLOADFILE + "/" + encodedId) + "' ");
                                    }
                                    if (isAjaxAction) {
                                        out.print("act-ajax='true' act-confirm='" +
                                                String.format(I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.operationConfirm"),
                                                        I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action." + qzRequest.getModelName() + "." + actionKey),
                                                        I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model." + qzRequest.getModelName())) + " " + originUnEncodedId
                                                + " ?' ");
                                    }
                                    %>
                                >
                                <i class="icon icon-<%=action.icon()%>"></i>
                                <%=I18n.getString(ServerXml.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName()), "model.action." + qzRequest.getModelName() + "." + actionKey)%>
                                </a>
                                <%
                            }
                            %>
                        </td>
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
                partLinkUri="<%=ConsoleUtil.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, ListModel.ACTION_NAME_LIST + "?markForAddCsrf")%>&<%=ListModel.PARAMETER_PAGE_NUM%>="
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
    $(".morecheck", getRestrictedArea()).click(function(){
        if (!$(this).prop("checked")) {
            $(this).removeAttr("checked", false);
            $(".allcheck", getRestrictedArea()).prop("checked", false);
        } else {
            $(this).prop("checked", "checked");
            var isAll = true;
            var morechecks = $(".morecheck", getRestrictedArea());
            for (var i = 0; i < morechecks.length; i++){
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
            url = str + "&<%=ListModel.FIELD_NAME_ID%>=" + params;
        } else {
            url = str + "?<%=ListModel.FIELD_NAME_ID%>=" + params;
        }
        $("#" + action, getRestrictedArea()).attr("href", url);
    }
</script>
