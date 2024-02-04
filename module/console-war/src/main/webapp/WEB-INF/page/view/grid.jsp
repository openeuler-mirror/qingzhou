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
        <%
        if (!indexToShow.isEmpty()) {
            %>
            <%@ include file="../fragment/filter_form.jsp" %>
            <%
        }
        %>

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
