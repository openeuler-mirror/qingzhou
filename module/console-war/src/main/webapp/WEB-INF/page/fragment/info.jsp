<%@ page pageEncoding="UTF-8" %>

<%
boolean hasId = PageBackendService.hasIDField(qzRequest);
boolean chartEnabled = !qzResponse.getDataList().isEmpty();
boolean isMonitor = MonitorModel.ACTION_NAME_MONITOR.equals(qzRequest.getActionName());

String encodedId = qzRequest.getId();
if (ConsoleSDK.needEncode(encodedId)) {
    encodedId = ConsoleSDK.encodeId(encodedId);
}
String url = PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.jsonView, MonitorModel.ACTION_NAME_MONITOR + (hasId ? "/" + encodedId : ""));
%>

<div class="infoPage" chartMonitor="<%=isMonitor && chartEnabled%>" data-url="<%=url%>">
    <%
    if (isMonitor && chartEnabled) {
        %>
        <div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
            <div class="panel-body" style="word-break: break-all">
                <div class="block-bg" container="chart" style="height: 600px;width: 100%"></div>
            </div>
        </div>
        <%
    }
    %>

    <%
    List<Map<String, String>> models = qzResponse.getDataList();
    if (!models.isEmpty()) {
        Map<String, String> allData = new HashMap<>();
        Map<String, String> monitorData = new HashMap<>();
        allData.putAll(models.get(0));
        if(models.size() > 1){
            monitorData = models.get(0);
            allData.putAll(models.get(1));
        }
        List<String> infoFieldMap = new ArrayList<>();
        if (isMonitor) {
            for (Map.Entry<String, ModelField> entry : modelManager.getMonitorFieldMap(qzRequest.getModelName()).entrySet()) {
                ModelField monitoringField = entry.getValue();
                if (!monitoringField.supportGraphicalDynamic() && !monitoringField.supportGraphical()) {
                    infoFieldMap.add(entry.getKey());
                }
            }
        } else {
            for (String field : modelManager.getFieldNames(qzRequest.getModelName())) {
                infoFieldMap.add(field);
            }
        }
        boolean hasItem = false;
        for (String fieldName : infoFieldMap) {
            if (monitorData.containsKey(fieldName)) {
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
                    <div class="panel-body" style="word-break: break-all;">
                        <table class="table home-table" style="margin-bottom: 1px;">
                            <%
                            for (String fieldName : infoFieldMap) {
                                if (monitorData.containsKey(fieldName)) {
                                    continue;
                                }

                                String msg = I18n.getString(menuAppName, "model.field.info." + qzRequest.getModelName() + "." + fieldName);
                                String info = "";
                                if (msg != null && !msg.startsWith("model.field.")) {
                                    info = "<span class='tooltips' data-tip='" + msg + "'><i class='icon icon-question-sign' data-tip-arrow=\"right\"></i></span>";
                                }
                                String fieldValue = allData.get(fieldName);
                                fieldValue = fieldValue != null ? fieldValue : "";
                                fieldValue = fieldValue.replace("\r\n", "<br>").replace("\n", "<br>")
                                        .replace("<", "&lt;").replace(">", "&gt;");
                                %>
                                <tr>
                                    <td class="home-field-info" field="<%=fieldName%>">
                                        <label for="<%=fieldName%>"><%=info%>&nbsp;
                                            <%=I18n.getString(menuAppName, "model.field." + qzRequest.getModelName() + "." + fieldName)%>
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
        if (!models.isEmpty() && models.size() > 1) {
            StringBuilder keysBuilder = new StringBuilder();
            keysBuilder.append("{");
            for (Map.Entry<String, String> e : models.get(0).entrySet()) {
                String key = e.getKey();
                String i18n = I18n.getString(menuAppName, "model.field." + qzRequest.getModelName() + "." + key);
                int i = key.indexOf(MonitorModel.MONITOR_EXT_SEPARATOR);
                if (i > 0) {
                    i18n = I18n.getString(menuAppName, "model.field." + qzRequest.getModelName() + "." + key.substring(0, i));
                    i18n = i18n + ":" + key.substring(i + 1);
                }
                keysBuilder.append("\"").append(key).append("\":\"").append(i18n).append("\",");
            }
            if (keysBuilder.indexOf(",") > 0) {
                keysBuilder.deleteCharAt(keysBuilder.lastIndexOf(","));
            }
            keysBuilder.append("}");
            out.print(keysBuilder.toString());
        }
        %>
    </textarea>
</div>

<div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
    <div class="form-btn">
        <a href="javascript:void(0);" onclick="tw.goback(this);"
           btn-type="goback" class="btn">
            <%=PageBackendService.getMasterAppI18NString( "page.cancel")%>
        </a>
    </div>
</div>