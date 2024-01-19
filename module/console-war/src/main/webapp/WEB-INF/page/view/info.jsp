<%@ page import="qingzhou.console.impl.ConsoleWarHelper" %>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    boolean hasId = PageBackendService.hasIDField(qzRequest);
    boolean chartEnabled = !qzResponse.getDataList().isEmpty();
    boolean isMonitor = MonitorModel.ACTION_NAME_MONITOR.equals(qzRequest.getActionName());

    String encodedId = qzRequest.getId();
    if (ConsoleSDK.needEncode(encodedId)) {
        encodedId = ConsoleSDK.encodeId(encodedId);
    }
    String url = ConsoleUtil.buildRequestUrl(request, response, qzRequest, ViewManager.jsonView, MonitorModel.ACTION_NAME_MONITOR + "/" + encodedId);
%>

<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <div class="infoPage" chartMonitor="<%=isMonitor && chartEnabled%>" data-url="<%=url%>">
        <%
            if (isMonitor && chartEnabled) {
        %>
        <div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
            <div class="panel-body" style="word-break: break-all">
                <div class="block-bg" container="chart" style="height: 600px;width: 100%"></div>
            </div>
        </div>
        <%}%>

        <%
            List<Map<String, String>> models = qzResponse.getDataList();
            if (!models.isEmpty()) {
                Map<String, String> modelData = models.get(0);
                List<String> fieldMap = new ArrayList<>();
                if (isMonitor) {
                    for (Map.Entry<String, ModelField> entry : modelManager.getMonitorFieldMap(qzRequest.getModelName()).entrySet()) {
                        ModelField monitoringField = entry.getValue();
                        if (!monitoringField.supportGraphicalDynamic() && !monitoringField.supportGraphical()) {
                            fieldMap.add(entry.getKey());
                        }
                    }
                } else {
                    for (String field : modelManager.getFieldNames(qzRequest.getModelName())) {
                        ModelField modelField = modelManager.getModelField(qzRequest.getModelName(), field);

                        //if (ConsoleUtil.isEffective(fieldName -> modelData.get(field), modelField.effectiveWhen())) {
                            fieldMap.add(field);
                        //} TODO
                    }
                }
                boolean hasItem = false;
                final List<Map<String, String>> infoMonitorDataList = qzResponse.getDataList();
                Map<String, String> infoMonitorData = infoMonitorDataList.isEmpty() ? null : infoMonitorDataList.get(0);
                for (String fieldName : fieldMap) {
                    if (infoMonitorData != null && infoMonitorData.containsKey(fieldName)) {
                        continue;
                    }
                    hasItem = true;
                    break;
                }
                if (hasItem) {
        %>
        <div class="block-bg">
            <div class="panel"
                 style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF; border: 0px;">
                <!--<div class="panel-heading" style="background-color: #FFFFFF; opacity:0.9;border-color:#EFEEEE; font-size:14px;height:50px;line-height:35px;font-weight:600;">
                    信息
                </div>-->
                <div class="panel-body" style="word-break: break-all;">
                    <table class="table home-table" style="margin-bottom: 1px;">
                        <%
                            List<Map<String, String>> dataList = qzResponse.getDataList();
                            Map<String, String> attachments = new HashMap<>();
                            if (dataList != null && !dataList.isEmpty()) {
                                attachments = dataList.get(0);
                            }
                            for (String fieldName : fieldMap) {
                                if (attachments.containsKey(fieldName)) {
                                    continue;
                                }

                                String msg = I18n.getString(qzRequest.getAppName(), "model.field.info." + qzRequest.getModelName() + "." + fieldName);
                                String info = "";
                                if (msg != null && !msg.startsWith("model.field.")) {
                                    info = "<span class='tooltips' data-tip='" + msg + "'><i class='icon icon-question-sign' data-tip-arrow=\"right\"></i></span>";
                                }
                                String fieldValue = modelData.get(fieldName);
                                fieldValue = fieldValue != null ? fieldValue : "";
                                fieldValue = fieldValue.replace("\r\n", "<br>").replace("\n", "<br>")
                                        .replace("<", "&lt;").replace(">", "&gt;");
                        %>
                        <tr>
                            <td class="home-field-info" field="<%=fieldName%>">
                                <label for="<%=fieldName%>"><%=info%>&nbsp;
                                    <%=I18n.getString(qzRequest.getAppName(), "model.field." + qzRequest.getModelName() + "." + fieldName)%>
                                </label>
                            </td>
                            <td style="word-break: break-all">
                                <%
                                    if (fieldValue.startsWith("http")) {
                                %>
                                <a target="_blank" href="<%=fieldValue%>">
                                    <%=fieldValue%>
                                </a>
                                <%
                                    } else {
                                        out.print(fieldValue);
                                    }
                                %>
                            </td>
                        </tr>
                        <%
                            }
                        %>
                    </table>
                </div>
            </div>
        </div>
        <%
                }
            }
        %>

        <textarea name="infoKeys" rows="3" disabled="disabled" style="display:none;">
        <%
            StringBuilder keysBuilder = new StringBuilder();
            List<Map<String, String>> dataList = qzResponse.getDataList();
            if (dataList != null && !dataList.isEmpty()) {
                keysBuilder.append("{");
                for (Map.Entry<String, String> e : dataList.get(0).entrySet()) {
                    String key = e.getKey();
                    String i18n = I18n.getString(qzRequest.getAppName(), "model.field." + qzRequest.getModelName() + "." + key);
                    int i = key.indexOf(MonitorModel.MONITOR_EXT_SEPARATOR);
                    if (i > 0) {
                        i18n = I18n.getString(qzRequest.getAppName(), "model.field." + qzRequest.getModelName() + "." + key.substring(0, i));
                        i18n = i18n + ":" + key.substring(i + 1);
                    }
                    keysBuilder.append("\"").append(key).append("\":\"").append(i18n).append("\",");
                }
                if (keysBuilder.indexOf(",") > 0) {
                    keysBuilder.deleteCharAt(keysBuilder.lastIndexOf(","));
                }
                keysBuilder.append("}");
            }
            out.print(keysBuilder.toString());
        %>
        </textarea>
    </div>

    <div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
        <div class="form-btn">
            <a href="javascript:void(0);" onclick="tw.goback(this);"
               btn-type="goback" class="btn">
                <!--<i class="icon icon-undo"></i>-->
                <%=PageBackendService.getMasterAppI18NString( "page.cancel")%>
            </a>
        </div>
    </div>

</div>
