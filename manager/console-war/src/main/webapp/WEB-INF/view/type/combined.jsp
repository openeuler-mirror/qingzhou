<%@ page pageEncoding="UTF-8" %>

<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
    <%@ include file="../fragment/breadcrumb.jsp" %>
    <%
        String contextPath = request.getContextPath();
        CombinedDataBuilder customizedDataObject = (CombinedDataBuilder) qzResponse.getCustomizedDataObject();
        List<Combined.CombinedData> dataList = customizedDataObject.getDataList();
        if (!dataList.isEmpty()) {
            for (Combined.CombinedData combinedData : dataList) {

                if (combinedData instanceof Combined.ShowData) {
                    CombinedDataBuilder.ShowDataImpl showData = (CombinedDataBuilder.ShowDataImpl) combinedData;
                    Map<String, String> infoData = showData.getShowData();
    %>
    <%@ include file="../fragment/info.jsp" %>
    <%
        }

        if (combinedData instanceof Combined.UmlData) {
            CombinedDataBuilder.UmlDataImpl umlDataImpl = (CombinedDataBuilder.UmlDataImpl) combinedData;
            String umlData = umlDataImpl.getUmlData();
            if (Utils.notBlank(umlData)) {
    %>
    <img style="width: 600px" src="<%=umlData%>" alt="<%=umlDataImpl.getHeader()%>">
    <%
            }
        }

        if (combinedData instanceof Combined.ListData) {
            CombinedDataBuilder.ListDataImpl listData = (CombinedDataBuilder.ListDataImpl) combinedData;
            String[] fieldNames = listData.getFieldNames();
            List<String[]> fieldValues = listData.getFieldValues();

    %>
    <h4><%=listData.getHeader()%>
    </h4>
    <table class="qz-data-list table table-striped table-hover list-table responseScroll">
        <thead>
        <tr style="height:20px;">
            <%
                for (String field : fieldNames) {
                    int listWidth = 100 / (fieldNames.length);
            %>
            <%-- 注意这个width末尾的 % 不能删除 %>% 不是手误 --%>
            <th style="width: <%=listWidth%>%"><%=field%>
            </th>
            <%
                }
            %>
        </tr>
        </thead>
        <tbody>
        <%
            for (String[] fieldValueArr : fieldValues) {
                if (fieldValueArr.length == 0) {
                    String dataEmpty = "<tr><td colspan='" + fieldNames.length + "' align='center'>"
                            + "<img src='" + contextPath + "/static/images/data-empty.svg' style='width:160px; height: 160px;'><br>"
                            + "<span style='font-size:14px; font-weight:600; letter-spacing: 2px;'>" + I18n.getKeyI18n("page.none") + "</span></td>";
                    out.print(dataEmpty);
                } else {
        %>
        <tr>
            <%
                for (int j = 0; j < fieldValueArr.length; j++) {
            %>
            <td><%=fieldValueArr[j]%>
            </td>
            <%
                }
            %>
        </tr>
        <%
                }
            }
        %>
        </tbody>
    </table>
    <%
                }
            }
        }

        if (SecurityController.isActionPermitted(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, currentUser)) {
    %>
    <div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
        <a class="btn"
           onclick="returnHref('<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, qingzhou.api.type.List.ACTION_LIST)%>')"
           href="javascript:void(0)">
            <%=I18n.getKeyI18n("page.return")%>
        </a>
    </div>
    <%
        }
    %>
</div>