<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv" overview="true">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <%-- 仪表盘 --%>
    <div class="panel" view="dashboard"
         data-url="<%=PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.jsonView , "os")%>"
         actName="<%=I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + ".os")%>"
         style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
        <div class="panel-heading"
             style="background-color: #FFFFFF; opacity:0.9;border-color:#EFEEEE; font-size:14px;height:50px;line-height:35px;font-weight:600;">
            <%=I18n.getString(menuAppName, "model.action." + "overview" + ".os")%>
        </div>
        <div class="forChart" style="margin:0 auto; display: flex; justify-content: center; flex-wrap: wrap;"></div>
        <div style="width:0px; height:0px; clear:both;"></div>
    </div>

    <%-- 柱状图 --%>
    <div class="panel" view="bars"
         data-url="<%=PageBackendService.buildRequestUrl(request, response,qzRequest, ViewManager.jsonView, ACTION_NAME_SERVER)%>"
         actName="<%=I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + ACTION_NAME_SERVER)%>"
         style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
        <div class="panel-heading"
             style="background-color: #FFFFFF; opacity:0.9;border-color:#EFEEEE; font-size:14px;height:50px;line-height:35px;font-weight:600;">
            <%=I18n.getString(menuAppName, "model.action.overview." + ACTION_NAME_SERVER)%>
        </div>
        <div class="forChart" style="margin:0 auto;display: flex;justify-content: center;flex-wrap: wrap;"></div>
        <div style="width:0px; height:0px; clear:both;"></div>
    </div>
</div>
