<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <%
        String url = PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG, Monitor.ACTION_MONITOR + (Utils.notBlank(encodedId) ? "/" + encodedId : ""));
    %>

    <div class="infoPage" chartMonitor="true" data-url="<%=url%>">
        <input type="hidden" name="monitorName" value="<%=(Utils.notBlank(encodedId) ? encodedId : "")%>">
        <div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
            <div class="panel-body" style="word-break: break-all">
                <div container="chart" style="height: 600px;width: 100%;"></div>
            </div>
        </div>

        <%
            List<Map<String, String>> dataList = qzResponse.getDataList();
            if (dataList.size() == 2) {
                Map<String, String> infoData = dataList.get(1);
        %>
        <%@ include file="../fragment/field.jsp" %>
        <textarea name="infoKeys" rows="3" disabled="disabled" style="display:none;">
        <%
            StringBuilder keysBuilder = new StringBuilder();
            keysBuilder.append("{");
            for (Map.Entry<String, String> e : dataList.get(0).entrySet()) {
                String key = e.getKey();
                String i18n = I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + key);
                keysBuilder.append("\"").append(key).append("\":[\"").append(i18n).append("\",\"");
                keysBuilder.append(I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + key)).append("\"],");
            }
            if (keysBuilder.indexOf(",") > 0) {
                keysBuilder.deleteCharAt(keysBuilder.lastIndexOf(","));
            }
            keysBuilder.append("}");
            out.print(keysBuilder.toString());
        %>
        </textarea>
        <%
            }
        %>
    </div>

    <%
        if (SecurityController.isActionPermitted(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, currentUser)) {
    %>
    <div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
        <div class="form-btn">
            <a class="btn" onclick="returnHref('<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, qingzhou.api.type.List.ACTION_LIST)%>')"
               href="javascript:void(0)">
                <%=I18n.getKeyI18n("page.return")%></a>
        </div>
    </div>
    <%
        }
    %>
</div>