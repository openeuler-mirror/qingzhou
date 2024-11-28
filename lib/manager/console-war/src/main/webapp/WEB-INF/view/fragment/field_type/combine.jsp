<%@ page pageEncoding="UTF-8" %>

<div onclick="addCombine(this,'<%=fieldName%>');"
     data-tip="<%=I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName)%>">
    <input id="<%=fieldName%>" type="text" name="<%=fieldName%>" value='<%=fieldValue%>'
           class="form-control"
           placeholder="<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>">
</div>

<div style="display: none" id="combine-group-<%=fieldName%>">
    <div class="row" >
    <%
        String fieldNameForTemp = fieldName;
        for (String f : modelField.getCombineFields()) {
            fieldName = f;
            ModelFieldInfo mfiTemp = modelInfo.getModelFieldInfo(fieldName);
            String inputTypeTemp = "";
            if (ValidationFilter.isSingleSelect(mfiTemp)) {
                inputTypeTemp = "select";
            }

    %>
    <div class='col-xs-4 list-page-padding-bottom' id="form-item-<%=fieldName%>" >
        <div class="input-control" type="<%=inputTypeTemp%>" >
            <%
                echoGroup = "";
                fieldValue = "";
                if (ValidationFilter.isSingleSelect(mfiTemp)) {
            %>
            <%@ include file="select.jsp" %>
            <%
            } else {
            %>
            <input type="text" name="<%=fieldName%>" value='<%=fieldValue%>'
                   class="form-control"
                   placeholder="<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>">
            <%
                }
            %>
        </div>
    </div>
    <%
        }
        fieldName = fieldNameForTemp;
    %>
        <span class="input-group-btn" style="padding-left: 10px;">
            <a class="btn" href="javascript:void(0);" onclick="delCombineGroup(this)">
                <i class="icon icon-trash"></i>
                <%=I18n.getKeyI18n("page.info.del")%>
            </a>
        </span>
    </div>
</div>
