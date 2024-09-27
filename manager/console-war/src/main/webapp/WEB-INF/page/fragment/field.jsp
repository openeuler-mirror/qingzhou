<%@ page pageEncoding="UTF-8" %>

<div class="infoPage">
	<%
		Map<String,List<String>> groupData = new LinkedHashMap<>();
		for (String fieldName : infoData.keySet()) {
			ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
			List<String> fieldNames = groupData.get(modelField.getGroup());
			if (fieldNames == null || fieldNames.isEmpty()){
				fieldNames = new LinkedList<>();
				groupData.put(modelField.getGroup(),fieldNames);
			}
			fieldNames.add(fieldName);
		}
		Double width = 99.7/groupData.keySet().size();
		int i = 0;
		String panelClass = "";
		for (String groupKey : groupData.keySet()){
			if (i % 2 == 0){
				panelClass = "panel-success";
			}else{
				panelClass = "panel-info";
			}
	%>
		<div class="panel <%=panelClass%>" style="border-radius: 2px; border-color:#EFEEEE;width: <%=width%>%;display: inline-block">
			<%
				if (!groupKey.isEmpty()){
			%>
			<div class="panel-heading"><%=groupKey%></div>
			<%
				}
			%>
			<div class="panel-body" style="word-break: break-all;">
				<table class="table home-table" style="margin-bottom: 1px;">
					<%
						for (String fieldName : groupData.get(groupKey)) {
							ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
							if (modelField == null) continue;

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
	<%
				i++;
			}
    %>