<%@ page pageEncoding="UTF-8" %>

<div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
    <div class="panel-body" style="word-break: break-all">
        <table class="table home-table" style="margin-bottom: 1px;">
            <%
            List<Map<String, String>> dataList = qzResponse.getDataList();
            if(dataList!=null && !dataList.isEmpty()){
                Map<String, String> data = dataList.get(0);
                for (String fieldName : PageBackendService.getModelManager(menuAppName).getFieldNames(qzRequest.getModelName())) {
                    %>
                    <tr>
                        <td class="home-field-info" field="<%=fieldName%>">
                            <label for="<%=fieldName%>"><%=I18n.getString(menuAppName, "model.field." + qzRequest.getModelName() + "." + fieldName)%></label>
                            <span class="tooltips" data-tip="<%=I18n.getString(menuAppName, "model.field.info." + qzRequest.getModelName() + "." + fieldName)%>" data-tip-arrow="right" style="line-height:25px;">
                                <i class="icon icon-question-sign"></i>
                            </span>
                        </td>
                        <td style="word-break: break-all">
                            <%
                            out.print(data.get(fieldName));
                            %>
                        </td>
                    </tr>
                    <%
                }
            }%>
        </table>
    </div>
</div>
