<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    String contextPath = request.getContextPath();
    if (qzRequest == null || qzResponse == null) {
        return; // for 静态源码漏洞扫描
    }

    final boolean hasId = ConsoleUtil.hasIDField(qzRequest);

    LinkedHashMap<String, ModelField> fieldInfos = new LinkedHashMap<>();
    String[] fieldNames = modelManager.getFieldNames(qzRequest.getModelName());
    for (String fieldName : fieldNames) {
        fieldInfos.put(fieldName, modelManager.getModelField(qzRequest.getModelName(), fieldName));
    }
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

    int totalSize = qzResponse.getTotalSize();
    int pageNum = qzResponse.getPageNum();
    int pageSize = qzResponse.getPageSize();
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
                                Options modelOptions = modelManager.getOptions(qzRequest.getModelName(), fieldName);
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
                               class="input-control-label-left"><%=I18n.getString(qzRequest.getAppName(), "model.field." + qzRequest.getModelName() + "." + fieldName)%>
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
                            <i class="icon icon-search"></i> <%=I18n.getString(Constants.MASTER_APP_NAME, "page.filter")%>
                        </a>
                    </span>
                </div>
            </div>
        </form>

        <hr style="margin-top: 4px;">

        <div class="table-tools tw-list-operate">
            <div class="tools-group">
                <%
                    boolean canAccess = (AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(), qzRequest.getModelName() + "/" + AddModel.ACTION_NAME_ADD, LoginManager.getLoginUser(session)));
                    ModelAction listCreateAction = modelManager.getModelAction(qzRequest.getModelName(), AddModel.ACTION_NAME_CREATE);
                    ModelAction listAddAction = modelManager.getModelAction(qzRequest.getModelName(), AddModel.ACTION_NAME_ADD);
                    if (canAccess && listCreateAction != null && listAddAction != null) {
                %>
                <a class="btn"
                   href="<%=ConsoleUtil.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, AddModel.ACTION_NAME_CREATE)%>">
                    <i class="icon icon-<%=listCreateAction.icon()%>"></i>
                    <%=I18n.getString(qzRequest.getAppName(), "model.action." + qzRequest.getModelName() + "." + AddModel.ACTION_NAME_CREATE)%>
                </a>
                <%
                    }

                    boolean downloadPermission = (AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(), qzRequest.getModelName() + "/" + DownloadModel.ACTION_NAME_DOWNLOADFILE, LoginManager.getLoginUser(session))
                            && AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(), qzRequest.getModelName() + "/" + DownloadModel.ACTION_NAME_DOWNLOADLIST, LoginManager.getLoginUser(session)));
                    final ModelAction downloadListModelAction = modelManager.getModelAction(qzRequest.getModelName(), DownloadModel.ACTION_NAME_DOWNLOADLIST);
                    if (downloadListModelAction != null && downloadPermission && Arrays.asList(modelManager.getActionNamesShowToListHead(qzRequest.getModelName())).contains(DownloadModel.ACTION_NAME_DOWNLOADLIST)) {
                %>
                <a style="margin-left:6px" class="btn" btn-type="<%=DownloadModel.ACTION_NAME_DOWNLOADLIST%>"
                   action-name="<%=DownloadModel.ACTION_NAME_DOWNLOADLIST%>"
                   href="<%= ConsoleUtil.isDisableDownload() ? "javascript:void(0);" : ConsoleUtil.buildRequestUrl(request, response, qzRequest, ViewManager.jsonView, DownloadModel.ACTION_NAME_DOWNLOADLIST)%>"
                        <%
                            out.print(ConsoleUtil.isDisableDownload() ? " disabled " : "" + " downloadfile='" + ConsoleUtil.buildRequestUrl(request, response, qzRequest, ViewManager.fileView, DownloadModel.ACTION_NAME_DOWNLOADFILE) + "' ");%>
                   out.print("act-ajax='true' act-confirm='" +
                String.format(I18n.getString(Constants.MASTER_APP_NAME, "page.operationConfirm"),
                I18n.getString(qzRequest.getAppName(), "model.action." + qzRequest.getModelName() + "." +
                DownloadModel.ACTION_NAME_DOWNLOADLIST),
                I18n.getString(qzRequest.getAppName(), "model." + qzRequest.getModelName()))
                + " ?' ");
                %>

                <i class="icon icon-<%=downloadListModelAction.icon()%>"></i>
                <%=I18n.getString(qzRequest.getAppName(), "model.action." + qzRequest.getModelName() + "." + DownloadModel.ACTION_NAME_DOWNLOADLIST)%>
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
                            String titleStr = I18n.getString(qzRequest.getAppName(), "model.action.info." + qzRequest.getModelName() + "." + actionKey);
                            if (StringUtil.notBlank(titleStr)) {
                                titleStr = "data-tip='" + titleStr + "'";
                            } else {
                                titleStr = "data-tip='" + I18n.getString(qzRequest.getAppName(), "model.action." + qzRequest.getModelName() + "." + actionKey) + "'";
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
                                        String.format(I18n.getString(Constants.MASTER_APP_NAME, "page.operationConfirm"),
                                                I18n.getString(qzRequest.getAppName(), "model.action." + qzRequest.getModelName() + "." + actionKey),
                                                I18n.getString(qzRequest.getAppName(), "model." + qzRequest.getModelName())) + " ?' ");
                            }
                        %>
                >
                    <i class="icon icon-<%=action.icon()%>"></i>
                    <%=I18n.getString(qzRequest.getAppName(), "model.action." + qzRequest.getModelName() + "." + actionKey)%>
                </a>
                <%
                        }
                    }
                %>
            </div>
        </div>

        <!-- grid page -->
        <div class="cards cards-borderless">
            <div class="col-md-4 col-sm-6 col-lg-3" style="margin-top:8px;">
                <div class="appCard">
                    <div class="left">
                        <div class="appLogo"><img src="<%=contextPath%>/static/images/apps/apache_tomcat.svg" alt="" width="80" height="80" /></div>
                        <div class="appVersion">
                            <div align="center">0.9.8-alpha1</div>
                        </div>
                    </div>

                    <div class="right">
                        <div class="appName">tomcatManager</div>
                        <div class="appInfo">
                            详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。
                            详细信息，详细信息，详细信息。详细信息，详细信息......
                        </div>
                        <div class="installUpgrade">
                            <a href="javascript:void(0);">部署</a>
                        </div>
                    </div>		
                </div>                                
            </div>
            <div class="col-md-4 col-sm-6 col-lg-3" style="margin-top:8px;">
                <div class="appCard">
                    <div class="left">
                        <div class="appLogo"><img src="<%=contextPath%>/static/images/apps/apache_rocketmq.svg" alt="" width="80" height="80" /></div>
                        <div class="appVersion">
                            <div align="center">0.9.8-alpha1</div>
                        </div>
                    </div>

                    <div class="right">
                        <div class="appName">rocketmqManager</div>
                        <div class="appInfo">
                            详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。
                            详细信息，详细信息，详细信息。详细信息，详细信息......
                        </div>
                        <div class="installUpgrade">
                            <a href="javascript:void(0);">升级</a>
                        </div>
                    </div>		
                </div>                                
            </div>

            <div class="col-md-4 col-sm-6 col-lg-3" style="margin-top:8px;">
                <div class="appCard">
                    <div class="left">
                        <div class="appLogo"><img src="<%=contextPath%>/static/images/apps/IzPack.png" alt="" width="80" height="80" /></div>
                        <div class="appVersion">
                            <div align="center">0.9.8-alpha1</div>
                        </div>
                    </div>

                    <div class="right">
                        <div class="appName">IzPackManager</div>
                        <div class="appInfo">
                            详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。
                            详细信息，详细信息，详细信息。详细信息，详细信息......
                        </div>
                        <div class="installUpgrade">
                            <a href="javascript:void(0);">部署</a>
                        </div>
                    </div>		
                </div>                                
            </div>

            <div class="col-md-4 col-sm-6 col-lg-3" style="margin-top:8px;">
                <div class="appCard">
                    <div class="left">
                        <div class="appLogo"><img src="<%=contextPath%>/static/images/apps/alibaba_nacos.svg" alt="" width="80" height="80" /></div>
                        <div class="appVersion">
                            <div align="center">0.9.8-alpha1</div>
                        </div>
                    </div>

                    <div class="right">
                        <div class="appName">NacosManager</div>
                        <div class="appInfo">
                            详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。
                            详细信息，详细信息，详细信息。详细信息，详细信息......
                        </div>
                        <div class="installUpgrade">
                            <a href="javascript:void(0);">部署</a>
                        </div>
                    </div>		
                </div>                                
            </div>
            <div class="col-md-4 col-sm-6 col-lg-3" style="margin-top:8px;">
                <div class="appCard">
                    <div class="left">
                        <div class="appLogo"><img src="<%=contextPath%>/static/images/apps/opensearch.svg" alt="" width="80" height="80" /></div>
                        <div class="appVersion">
                            <div align="center">0.9.8-alpha1</div>
                        </div>
                    </div>

                    <div class="right">
                        <div class="appName">OpenSearchManager</div>
                        <div class="appInfo">
                            详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。
                            详细信息，详细信息，详细信息。详细信息，详细信息......
                        </div>
                        <div class="installUpgrade">
                            <a href="javascript:void(0);">部署</a>
                        </div>
                    </div>		
                </div>                                
            </div>
            <div class="col-md-4 col-sm-6 col-lg-3" style="margin-top:8px;">
                <div class="appCard">
                    <div class="left">
                        <div class="appLogo"><img src="<%=contextPath%>/static/images/apps/redis.svg" alt="" width="80" height="80" /></div>
                        <div class="appVersion">
                            <div align="center">0.9.8-alpha1</div>
                        </div>
                    </div>

                    <div class="right">
                        <div class="appName">RedisManager</div>
                        <div class="appInfo">
                            详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。详细信息，详细信息，详细信息。
                            详细信息，详细信息，详细信息。详细信息，详细信息......
                        </div>
                        <div class="installUpgrade">
                            <a href="javascript:void(0);">部署</a>
                        </div>
                    </div>		
                </div>                                
            </div>
        </div>

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
            url = str + "&<%=ListModel.FIELD_NAME_ID%>=" + params;
        } else {
            url = str + "?<%=ListModel.FIELD_NAME_ID%>=" + params;
        }
        $("#" + action, getRestrictedArea()).attr("href", url);
    }
</script>
