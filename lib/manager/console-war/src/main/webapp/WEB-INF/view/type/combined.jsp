<%@ page pageEncoding="UTF-8" %>

<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
    <%@ include file="../fragment/breadcrumb.jsp" %>
    <%
        String contextPath = request.getContextPath();
        CombinedDataBuilder customizedDataObject = (CombinedDataBuilder) qzResponse.getAppData();
        Collection<Combined.CombinedData> dataList = customizedDataObject.data;
        if (!dataList.isEmpty()) {
            for (Combined.CombinedData combinedData : dataList) {
                if (combinedData instanceof Combined.ShowData) {
                    CombinedDataBuilder.Show showData = (CombinedDataBuilder.Show) combinedData;
                    ModelInfo showDataModelInfo = Objects.requireNonNull(SystemController.getModelInfo(qzApp, showData.model));
                    Map<String, String> infoData = showData.data;
                    Map<String, List<String>> groupedFields = PageUtil.groupedFields(infoData.keySet(), showDataModelInfo);
                    boolean hasGroup = PageUtil.hasGroup(groupedFields);

                    Double width = 99.6 / Math.min(3, groupedFields.size());
                    ItemData[] groupInfos = showDataModelInfo.getGroupInfos();
                    int i = 0;
                    for (String groupKey : groupedFields.keySet()) {
                        ItemData gInfo = Arrays.stream(groupInfos).filter(groupInfo -> groupInfo.getName().equals(groupKey)).findAny().orElse(PageUtil.OTHER_GROUP);
                        String panelClass = i % 2 == 0 ? "panel-success" : "panel-info";
                        i++;
    %>
    <div class="panel <%=panelClass%>"
         style="border-radius: 2px; border-color:#EFEEEE;width: <%=width%>;display: inline-block">
        <%
            if (hasGroup) {
        %>
        <div class="panel-heading"><%=I18n.getStringI18n(gInfo.getI18n())%>
        </div>
        <%
            }
        %>
        <div class="panel-body" style="word-break: break-all;">
            <table class="table table-striped table-hover home-table" style="margin-bottom: 1px;">
                <%
                    for (String fieldName : groupedFields.get(groupKey)) {
                        ModelFieldInfo modelField = showDataModelInfo.getModelFieldInfo(fieldName);

                        String msg = I18n.getModelI18n(qzApp, "model.field.info." + showData.model + "." + fieldName);
                        String info = "";
                        if (msg != null && !msg.startsWith("model.field.")) {
                            info = "<span class='tooltips' data-tip='" + msg + "'><i class='icon icon-info-sign' data-tip-arrow=\"right\"></i></span>";
                        }
                        String fieldValue = infoData.get(fieldName);
                        fieldValue = fieldValue != null ? fieldValue : "";
                        if (InputType.markdown != modelField.getInputType()) {
                            fieldValue = fieldValue.replace("\r\n", "<br>").replace("\n", "<br>").replace("<", "&lt;").replace(">", "&gt;");
                        }
                %>
                <tr row-item="<%=fieldName%>">
                    <td class="home-field-info" field="<%=fieldName%>">
                        <label><%=info%>&nbsp;
                            <%=I18n.getModelI18n(qzApp, "model.field." + showData.model + "." + fieldName)%>
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
                                out.print(PageUtil.getInputTypeStyle(fieldValue, qzRequest, modelField));
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

    <%
            }
        }

        if (combinedData instanceof Combined.UmlData) {
            CombinedDataBuilder.Uml umlDataImpl = (CombinedDataBuilder.Uml) combinedData;
            String umlData = umlDataImpl.data;
            if (Utils.notBlank(umlData)) {
    %>
    <img style="vertical-align: top;margin-top: -5px;" src="data:image/svg+xml;base64,<%=umlData%>">
    <%
            }
        }

        if (combinedData instanceof Combined.ListData) {
            CombinedDataBuilder.List listData = (CombinedDataBuilder.List) combinedData;
            String[] fieldNames = listData.fields;
            List<String[]> fieldValues = listData.values;

    %>
    <h4><%=listData.header%>
    </h4>
    <table class="qz-data-list table table-striped table-hover list-table responseScroll">
        <thead>
        <tr style="height:20px;">
            <%
                for (String field : fieldNames) {
                    int listWidth = 100 / (fieldNames.length);
            %>
            <%-- 注意这个width末尾的 % 不能删除 %>% 不是手误 --%>
            <th style="width: <%=listWidth%>%"><%=I18n.getModelI18n(qzApp, "model.field." + listData.model + "." + field)%>
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
    %>

    <%@ include file="../fragment/back.jsp" %>
</div>
