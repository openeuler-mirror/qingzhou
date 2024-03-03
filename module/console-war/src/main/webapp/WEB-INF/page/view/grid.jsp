<%@ page import="qingzhou.api.type.Listable" %>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
String contextPath = request.getContextPath();
if (qzRequest == null || qzResponse == null) {
    return; // for 静态源码漏洞扫描
}

LinkedHashMap<String, ModelFieldData> fieldInfos = new LinkedHashMap<>();
String[] fieldNames = modelManager.getFieldNames(qzRequest.getModelName());
for (String fieldName : fieldNames) {
    fieldInfos.put(fieldName, modelManager.getModelField(qzRequest.getModelName(), fieldName));
}
int num = -1;
List<Integer> indexToShow = new ArrayList<>();
for (Map.Entry<String, ModelFieldData> e : fieldInfos.entrySet()) {
    num++;
    ModelFieldData modelField = e.getValue();
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

        <!-- grid page -->
        <div class="cards cards-borderless">
            <div class="apps">
                <%
                List<Map<String, String>> modelDataList = qzResponse.getDataList();
                for (int ii = 0; ii < modelDataList.size(); ii++) {
                    Map<String, String> app = modelDataList.get(ii);
                    String appId = app.getOrDefault("id", "0");
                    String appName = app.getOrDefault("appName", "");
                    String appLogo = app.getOrDefault("appLogo", "");
                    String appVersion = app.getOrDefault("appVersion", "");
                    String appDetail = app.getOrDefault("appDetail", "");
                    String installedVersion = app.getOrDefault("installedVersion", "");
                    String tempHtml = "";
                    if (installedVersion == null || "".equals(installedVersion)) {
                        tempHtml = "<a href=\"javascript:void(0);\" style=\"padding: 3px 3px;\">部署</a>";
                    } else {
                        tempHtml = "<a href=\"javascript:void(0);\" style=\"padding: 3px 3px;\">卸载</a>";
                        if (!installedVersion.equals(appVersion)) {
                            tempHtml += "<a href=\"javascript:void(0);\" style=\"padding: 3px 3px;\">升级</a>";
                        }
                    }
                %>
                <div class="app">
                    <div class="app-card">
                        <table class="app-table">
                            <tr>
                                <td colspan="2" class="app-name"><%=appName%></td>
                            </tr>
                            <tr>
                                <td class="app-left">
                                    <div class="app-left-div">
                                        <a href="<%=contextPath%>/?id=<%=appId%>"><img src="<%=contextPath%>/static/images/apps/<%=appLogo%>" alt="" class="app-logo" /></a>
                                        <div class="app-version"><%=appVersion%></div>
                                        <div class="app-install-upgrade">
                                            <%=tempHtml%>
                                        </div>
                                    </div>
                                </td>
                                <td class="app-detail">
                                    <%=appDetail%>
                                </td>
                            </tr>
                        </table>
                    </div>
                </div>
                <%
                }
                %>
            </div>

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
</div>
