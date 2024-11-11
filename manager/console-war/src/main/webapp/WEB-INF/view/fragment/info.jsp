<%@ page pageEncoding="UTF-8" %>

<%
        if (infoData == null) {
            infoData = new HashMap<>();
        }
	Map<String, List<String>> groupedFields = PageUtil.groupedFields(infoData.keySet(), modelInfo);
	boolean hasGroup = PageUtil.hasGroup(groupedFields);

	Double width = 99.6 / Math.min(3, groupedFields.size());
	ItemInfo[] groupInfos = modelInfo.getGroupInfos();
	int i = 0;
	for (String groupKey : groupedFields.keySet()) {
		ItemInfo gInfo = Arrays.stream(groupInfos).filter(groupInfo -> groupInfo.getName().equals(groupKey)).findAny().orElse(PageUtil.OTHER_GROUP);
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
					ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);

					String needHidden = "";
					if (modelField.isHidden()) {
						needHidden = "display:none";
					}
					String msg = I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName);
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
			<tr row-item="<%=fieldName%>" style="<%=needHidden%>">
				<td class="home-field-info" field="<%=fieldName%>">
					<label><%=info%>&nbsp;
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
							out.print(PageUtil.getInputTypeStyle(fieldValue, qzApp, modelInfo, modelField));
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
%>

<script>
    $(document).ready(function () {
        //为所有class=markdownview的div内容做转化
        const markdownViews = document.querySelectorAll('div.markdownview');

        // 使用 forEach 循环遍历每个元素
        markdownViews.forEach((element) => {
            // 在这里对每个元素进行操作
            let data = element.innerHTML;
            element.innerHTML = marked.parse(data);
        });
    });
</script>
