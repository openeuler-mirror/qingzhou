<%@ page pageEncoding="UTF-8" %>

<div class="tab-content" style="padding-top: 12px; padding-bottom: 12px;">
    <%
        for (String fieldName : modelInfo.getFormFieldNames()) {
            if (!Utils.contains(action.getLinkFields(), fieldName)) {
                continue;
            }
            // 避免编译报错，放入这个循环里，是避免和 list.jsp 中 的同名变量冲突
            java.util.List<String> passwordFields = new ArrayList<>();
            String echoGroup = "";

            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);

            String fieldValue = modelData.get(fieldName);
            if (fieldValue == null) {
                fieldValue = "";
            }
            java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(modelField.getSeparator()));
    %>
    <div class="form-group" id="form-item-<%=fieldName%>">
        <label for="<%=fieldName%>" class="col-sm-4">
            <%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>
            <%
                String fieldInfo = I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName);
                if (fieldInfo != null) {
                    out.print("<span class='tooltips' data-tip='" + fieldInfo + "' data-tip-arrow='bottom-right'><i class='icon icon-question-sign'></i></span>");
                }
            %>
        </label>
        <div class="col-sm-5">
            <%@ include file="../fragment/field_type.jsp" %>
            <label class="tw-error-info"></label>
        </div>
    </div>
    <%
        }
    %>
</div>