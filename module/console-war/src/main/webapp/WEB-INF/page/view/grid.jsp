<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    String contextPath = request.getContextPath();
    if (qzRequest == null || qzResponse == null) {
        return; // for 静态源码漏洞扫描
    }

    LinkedHashMap<String, ModelField> fieldInfos = new LinkedHashMap<>();
    String[] fieldNames = modelManager.getFieldNames(qzRequest.getModelName());
    for (String fieldName : fieldNames) {
        fieldInfos.put(fieldName, modelManager.getModelField(qzRequest.getModelName(), fieldName));
    }
    int num = -1;
    List<Integer> indexToShow = new ArrayList<>();
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
              action="<%=PageBackendService.encodeURL( response, ViewManager.htmlView + "/" + qzRequest.getModelName() + "/" + ListModel.ACTION_NAME_LIST)%>">
            <div class="row filterForm" style="margin-top: 10px;">
                <%
                    for (Integer i : indexToShow) {
                        String fieldName = modelManager.getFieldName(qzRequest.getModelName(), i);
                        List<Option> modelOptionsEntry = null;
                        if (PageBackendService.isFilterSelect(qzRequest, i)) {
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
                               class="input-control-label-left"><%=I18n.getString(menuAppName, "model.field." + qzRequest.getModelName() + "." + fieldName)%>
                        </label>
                    </div>
                </div>
                <%
                    }
                %>
                <div class="col-md-3 search-btn" style="margin-bottom: 16px;">
                    <span class="input-group-btn col-md-4" style="width: 18%;padding-left:0px;">
                        <a class="btn"
                           href="<%=PageBackendService.buildRequestUrl(request, response, qzRequest,ViewManager.htmlView,ListModel.ACTION_NAME_LIST)%>"
                           form="filterForm">
                            <i class="icon icon-search"></i> <%=PageBackendService.getMasterAppI18NString( "page.filter")%>
                        </a>
                    </span>
                </div>
            </div>
        </form>

        <hr style="margin-top: 4px;">

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
                partLinkUri="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.htmlView, ListModel.ACTION_NAME_LIST + "?markForAddCsrf")%>&<%=ListModel.PARAMETER_PAGE_NUM%>="
                style="margin-left:33%;color:black;margin-bottom:6px;">
            </ul>
        </div>
    </div>
</div>
