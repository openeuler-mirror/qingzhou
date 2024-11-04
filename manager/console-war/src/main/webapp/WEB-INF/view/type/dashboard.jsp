<%@ page import="qingzhou.api.type.Dashboard" %>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <%
        String url = PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG, Dashboard.ACTION_DASHBOARD + (Utils.notBlank(encodedId) ? "/" + encodedId : ""));
    %>

    <div class="dashboardPage" data-url="<%=url%>">
        <div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
            <div class="panel-body" style="word-break: break-all">
                <div class="basicData" container="basicData" style="width: 100%;"></div>
            </div>
        </div>
        <%-- 仪表盘 --%>
        <div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
            <div class="panel-body" style="word-break: break-all">
                <div class="gaugeChart" container="gaugeChart" style="width: 100%;"></div>
            </div>
        </div>
        <%-- 柱状图 --%>
        <div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
            <div class="panel-body" style="word-break: break-all">
                <div class="histogramChart" container="histogramChart" style="width: 100%;"></div>
            </div>
        </div>
        <%-- 共享数据集 --%>
        <div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
            <div class="panel-body" style="word-break: break-all">
                <div class="shareDatasetChart" container="shareDatasetChart" style="width: 100%;"></div>
            </div>
        </div>
    </div>
</div>