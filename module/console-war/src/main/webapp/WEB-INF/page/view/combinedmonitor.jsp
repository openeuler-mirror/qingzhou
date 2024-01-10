<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    String encodedId = qzRequest.getId();
    if (ConsoleSDK.needEncode(encodedId)) {
        encodedId = ConsoleSDK.encodeId(encodedId);
    }
    String url = PageBackendService.encodeURL(request, response, ViewManager.jsonView + "/" + qzRequest.getTargetType() + "/"+ qzRequest.getTargetName() +"/" + qzRequest.getModelName() + "/" + MonitorModel.ACTION_NAME_MONITOR + "/" + encodedId);
%>

<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <div class="infoPage" chartMonitor="true" data-url="<%=url%>">

        <div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
            <div class="panel-body" style="word-break: break-all">
                <div class="block-bg" container="chart" style="height: 600px;width: 100%"></div>
            </div>
        </div>

        <textarea name="infoKeys" rows="3" disabled="disabled" style="display:none;">
        <%
            StringBuilder keysBuilder = new StringBuilder();
            keysBuilder.append("{");

            for (Map<String, String> data : qzResponse.getDataList()) {
                for (Map.Entry<String, String> e : data.entrySet()) {
                    String key = e.getKey();
                    int m = key.indexOf(ConsoleConstants.MONITOR_MODEL_SEPARATOR);

                    String fieldAndId = key.substring(m + 1);
                    String fieldName = fieldAndId;
                    String fieldDataId = "";
                    int f = fieldAndId.indexOf(ConsoleConstants.MONITOR_EXT_SEPARATOR);
                    if (f > 0) {
                        fieldName = fieldAndId.substring(0, f);
                        fieldDataId = fieldAndId.substring(f + 1);
                    }

                    String theModelName = key.substring(0, m);

                    String i18n = "";
                    if (StringUtil.notBlank(fieldDataId)) {
                        i18n = I18n.getString(qzRequest.getAppName(), "model." + theModelName) + "(" + fieldDataId + "):" + I18n.getString(qzRequest.getAppName(), "model.field." + theModelName + "." + fieldName);
                    } else {
                        i18n = I18n.getString(qzRequest.getAppName(), "model." + theModelName) + ":" + I18n.getString(qzRequest.getAppName(), "model.field." + theModelName + "." + fieldName);
                    }
                    keysBuilder.append("\"").append(key).append("\":\"").append(i18n).append("\",");
                }
            }

            if (keysBuilder.indexOf(",") > 0) {
                keysBuilder.deleteCharAt(keysBuilder.lastIndexOf(","));
            }
            keysBuilder.append("}");
            out.print(keysBuilder.toString());
        %>
        </textarea>
    </div>

    <div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
        <div class="form-btn">
            <a href="javascript:void(0);" onclick="tw.goback(this);" btn-type="goback" class="btn">
                <%=PageBackendService.getMasterAppI18NString( "page.cancel")%>
            </a>
        </div>
    </div>
</div>
