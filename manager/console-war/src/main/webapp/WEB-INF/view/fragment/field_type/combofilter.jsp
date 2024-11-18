<%@ page pageEncoding="UTF-8" %>

<div style="width: 80%; float: left">
    <input id="<%=fieldName%>" type="text" name="<%=fieldName%>" value='<%=fieldValue%>'
           class="form-control"
           placeholder="<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>">
</div>

<span class="input-group-btn" style="width: 18%;float: right;">
    <a class="btn" onclick="addFilterItemGroup(this,'<%=fieldName%>')" href="javascript:void(0);"
       data-tip="<%=I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName)%>">
        <i class="icon icon-plus-sign"></i> <%=I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName)%>
    </a>
</span>

<div style="display: none" id="filter-group-<%=fieldName%>">
    <%
        String fieldNameForTemp = fieldName;
        for (String f : modelField.getComboFields()) {
            fieldName = f;
            ModelFieldInfo mfiTemp = modelInfo.getModelFieldInfo(fieldName);
            String inputTypeTemp = "";
            if (ValidationFilter.isSingleSelect(mfiTemp)) {
                inputTypeTemp = "select";
            }

    %>
    <div class='col-xs-4 list-page-padding-bottom'>
        <div class="input-control" id="form-item-<%=fieldName%>" type="<%=inputTypeTemp%>">
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
</div>
