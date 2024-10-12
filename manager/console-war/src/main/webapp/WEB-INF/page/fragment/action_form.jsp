<%@ page pageEncoding="UTF-8" %>

<%
	java.util.List<String> passwordFields = new ArrayList<>();
	Map<String, Map<String, ModelFieldInfo>> formGroup = modelInfo.getFormGroupedFields();
%>

<div class="block-bg">
    <div class="tab-content" style="padding-top: 12px; padding-bottom: 12px;">
        <%
            for (Map.Entry<String, Map<String, ModelFieldInfo>> groupInfo : formGroup.entrySet()) {
                Map<String, ModelFieldInfo> groupFieldMap = groupInfo.getValue();
                for (Map.Entry<String, ModelFieldInfo> e : groupFieldMap.entrySet()) {
                    String fieldName = e.getKey();
                    if (!Utils.contains(action.getFields(), fieldName)) {
                        continue;
                    }

					ModelFieldInfo modelField = e.getValue();

                    String echoGroup = String.join(",", modelField.getEchoGroup());
					if (!"".equals(echoGroup)) {
						echoGroup = "echoGroup='" + echoGroup.trim() + "'";
					}

                    String readonly = "";
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
            }
        %>
    </div>
</div>

