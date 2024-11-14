<%@ page pageEncoding="UTF-8" %>

<div class="tab-content" style="padding-top: 12px; padding-bottom: 12px;">
	<%
		for (Map.Entry<String, String> afEntry : actionFormData.entrySet()) {
			String fieldName = afEntry.getKey();
			String fieldValue = afEntry.getValue();
			ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
			java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(modelField.getSeparator()));
			java.util.List<String> passwordFields = new ArrayList<>();
			String echoGroup = "";

	%>
	<div class="form-group" id="form-item-<%=fieldName%>">
		<label class="col-sm-4">
			<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>
			<%
				String fieldInfo = I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName);
				if (fieldInfo != null) {
					out.print("<span class='tooltips' data-tip='" + fieldInfo + "' data-tip-arrow='bottom-right'><i class='icon icon-info-sign'></i></span>");
				}
			%>
		</label>
		<div class="col-sm-5" type="<%=modelField.getInputType().name()%>">
			<%@ include file="../fragment/field_type.jsp" %>
		</div>
	</div>
	<%
		}
	%>
</div>
<div style="margin-top: 15px; height: 64px; text-align: center;">
	<div class="form-btn">
		<input type="submit" class="btn" value='<%=I18n.getKeyI18n("page.confirm")%>'>
	</div>
</div>