<%@ page pageEncoding="UTF-8" %>

<div class="infoPage">
	<%
	List<Map<String, String>> dataList = qzResponse.getDataList();
	if (!dataList.isEmpty()) {
		Map<String, String> infoData = new HashMap<>();
		if (dataList.size() == 1) {
			infoData.putAll(dataList.get(0));
		}else if(dataList.size() > 1){
			infoData.putAll(dataList.get(1));
		}
		if (!infoData.isEmpty()) {
	%>
		<div class="block-bg">
			<div class="panel" style="border-radius: 2px; border-color:#EFEEEE;  border: 0px;">
				<div class="panel-body" style="word-break: break-all;">
					<table class="table home-table" style="margin-bottom: 1px;">
						<%
						for (String fieldName : infoData.keySet()) {
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
									ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
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