<%@ page pageEncoding="UTF-8" %>

<%
    for (String group : new String[]{ConsoleUtil.GROUP_NAME_PRODUCT}) {
%>
<div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
    <div class="panel-heading"
         style="background-color: #FFFFFF; opacity:0.9;border-color:#EFEEEE; font-size:14px;height:50px;line-height:35px;font-weight:600;">
        <%=I18n.getString(qzRequest.getAppName(), "field.group." + group)%>
    </div>
    <div class="panel-body" style="word-break: break-all">
        <table class="table home-table" style="margin-bottom: 1px;">
            <%
                for (String fieldName : modelManager.getFieldNamesByGroup(qzRequest.getModelName(), group)) {
            %>
            <tr>
                <td class="home-field-info" field="<%=fieldName%>">
                    <label for="<%=fieldName%>"><%=I18n.getString(qzRequest.getAppName(), "model.field." + qzRequest.getModelName() + "." + fieldName)%></label>
                    <span class="tooltips" data-tip="<%=I18n.getString(qzRequest.getAppName(), "model.field.info." + qzRequest.getModelName() + "." + fieldName)%>" data-tip-arrow="right" style="line-height:25px;">
                        <i class="icon icon-question-sign"></i>
                    </span>
                </td>
                <td style="word-break: break-all">
                    <%
                        List<Map<String, String>> dataList = qzResponse.modelData().getDataList();
                        if (!dataList.isEmpty()) {
                            out.print(dataList.get(0).get(fieldName));
                        }
                    %>
                </td>
            </tr>
            <%
                }%>
        </table>
    </div>
</div>
<%}%>
