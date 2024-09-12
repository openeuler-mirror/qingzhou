<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <%
        boolean chartEnabled = !qzResponse.getDataList().isEmpty();
        boolean isMonitor = Monitorable.ACTION_MONITOR.equals(qzAction);
        String url = PageBackendService.buildRequestUrl(request, response, qzRequest, DeployerConstants.jsonView, Monitorable.ACTION_MONITOR + (Utils.notBlank(encodedId) ? "/" + encodedId : ""));
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
        List<Map<String, String>> dataList = qzResponse.getDataList();
        if (!dataList.isEmpty() && dataList.size() > 1) {
            Map<String, String> infoData = new HashMap<>();
            infoData.putAll(dataList.get(1));
            if(!infoData.isEmpty()){
        %>
        <div class="block-bg">
            <div class="panel" style="border-radius: 2px; border-color:#EFEEEE;  border: 0px;">
                <div class="panel-body" style="word-break: break-all;">
                    <table class="table home-table" style="margin-bottom: 1px;">
                        <%
                        for (String fieldName : infoData.keySet()) {
                            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
                            if(modelField == null){
                                continue;
                            }
                            String msg = I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName);
                            String info = "";
                            if (msg != null && !msg.startsWith("model.field.")) {
                                info = "<span class='tooltips' data-tip='" + msg + "'><i class='icon icon-question-sign' data-tip-arrow=\"right\"></i></span>";
                            }
                            String fieldValue = infoData.get(fieldName);
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
        if (!dataList.isEmpty() && dataList.size() > 1) {
            StringBuilder keysBuilder = new StringBuilder();
            keysBuilder.append("{");
            for (Map.Entry<String, String> e : dataList.get(0).entrySet()) {
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
    if (request.getAttribute("comeFromIndexPage") == null) {
    %>
    <div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
        <div class="form-btn">
            <a href="javascript:void(0);" onclick="tw.goback(this);" btn-type="goback" class="btn">
                <%=I18n.getKeyI18n("page.return")%>
            </a>
        </div>
    </div>
    <%
    }
    %>

</div>