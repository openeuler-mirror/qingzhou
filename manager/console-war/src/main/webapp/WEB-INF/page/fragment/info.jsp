<%@ page pageEncoding="UTF-8" %>

<%
    boolean existId = PageBackendService.hasIDField(qzRequest);
    boolean chartEnabled = !qzResponse.getDataList().isEmpty();
    boolean isMonitor = DeployerConstants.ACTION_MONITOR.equals(qzAction);

    String encodedId = PageBackendService.encodeId(qzRequest.getId());
    String url = PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.jsonView, DeployerConstants.ACTION_MONITOR + (existId ? "/" + encodedId : ""));
%>

<div class="infoPage" chartMonitor="<%=isMonitor && chartEnabled%>" data-url="<%=url%>">
    <%
        if (isMonitor && chartEnabled) {
    %>
    <div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
        <div class="panel-body" style="word-break: break-all">
            <div class="block-bg" container="chart" style="height: 600px;width: 100%;"></div>
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
            if (models.size() > 1) {
                monitorData = models.get(0);
                allData.putAll(models.get(1));
            }
            List<String> infoFieldMap = new ArrayList<>();
            for (String fieldName : modelInfo.getFormFieldNames()) {
                infoFieldMap.add(fieldName);
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
             style="border-radius: 2px; border-color:#EFEEEE;  border: 0px;">
            <div class="panel-body" style="word-break: break-all;">
                <table class="table home-table" style="margin-bottom: 1px;">
                    <%
                        for (String fieldName : infoFieldMap) {
                            if (monitorData.containsKey(fieldName)) {
                                continue;
                            }

                            String msg = I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName);
                            String info = "";
                            if (msg != null && !msg.startsWith("model.field.")) {
                                info = "<span class='tooltips' data-tip='" + msg + "'><i class='icon icon-question-sign' data-tip-arrow=\"right\"></i></span>";
                            }
                            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
                            String fieldValue = allData.get(fieldName);
                            fieldValue = fieldValue != null ? fieldValue : "";
                            fieldValue = fieldValue.replace("\r\n", "<br>").replace("\n", "<br>").replace("<", "&lt;").replace(">", "&gt;");
                    %>
                    <tr row-item="<%=fieldName%>">
                        <td class="home-field-info" field="<%=fieldName%>">
                            <label for="<%=fieldName%>"><%=info%>&nbsp;
                                <%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>
                            </label>
                        </td>
                        <td style="word-break: break-all" field-val="<%=fieldValue%>">
                            <%
                                if (fieldValue.startsWith("http") && !fieldValue.startsWith("http-")) {
                            %>
                            <a target="_blank" href="<%=fieldValue%>">
                                <%=fieldValue%>
                            </a>
                            <%
                                } else {
                                    if (FieldType.markdown.name().equals(modelField.getType())) {
                                        out.print("<div class=\"markedview\"></div><textarea name=\"" + fieldName
                                                + "\" class=\"markedviewText\" rows=\"3\">" + fieldValue + "</textarea>");
                                    } else {
                                        out.print(fieldValue);
                                    }
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
                    String i18n = I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + key);
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
    <textarea name="eventConditionsInfoPage" rows="3" disabled="disabled" style="display:none;">
        <%
            // added by yuanwc for: ModelField 注解 effectiveWhen()
            StringBuilder conditionBuilder = new StringBuilder();
            conditionBuilder.append("{");
            Map<String, String> conditions = PageBackendService.modelFieldShowMap(qzRequest);
            for (Map.Entry<String, String> e : conditions.entrySet()) {
                //e.getValue().replace(/\&\&/g, '&').replace(/\|\|/g, '|');
                conditionBuilder.append("'").append(e.getKey()).append("' : '")
                        .append(e.getValue().replaceAll("\\&\\&", "&").replaceAll("\\|\\|", "|")).append("',");
            }
            if (conditionBuilder.indexOf(",") > 0) {
                conditionBuilder.deleteCharAt(conditionBuilder.lastIndexOf(","));
            }
            conditionBuilder.append("}");
            out.print(conditionBuilder.toString());
        %>
    </textarea>
</div>

<%
    if (request.getAttribute("indexPageFlag") == null) {
%>
<div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
    <div class="form-btn">
        <a href="javascript:void(0);" onclick="tw.goback(this);" btn-type="goback" class="btn" pg="info.jsp">
            <%=I18n.getKeyI18n("page.cancel")%>
        </a>
    </div>
</div>
<%
    }
%>