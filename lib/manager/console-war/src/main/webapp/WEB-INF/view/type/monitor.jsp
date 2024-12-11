<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<%
		String url = PageUtil.buildRequestUrl(request, response, qzRequest, JsonView.FLAG, Monitor.ACTION_MONITOR + (Utils.notBlank(encodedId) ? "/" + encodedId : ""));

		Map<String, String> numericData = new LinkedHashMap<>();
		Map<String, String> basicData = new LinkedHashMap<>();

		String[] monitorFieldNames = modelInfo.getMonitorFieldNames();
		for (String fieldName : monitorFieldNames) {
			String val = ((Map<String, String>) qzResponse.getInternalData()).get(fieldName);
			if (val == null) continue;
			ModelFieldInfo monitorField = modelInfo.getModelFieldInfo(fieldName);
			if (monitorField.isNumeric()) {
				numericData.put(fieldName, val);
			} else {
				basicData.put(fieldName, val);
			}
		}
	%>

	<div class="infoPage" chartMonitor="true" autoRefresh="true" data-url="<%=url%>">
		<input type="hidden" name="monitorName" value="<%=(Utils.notBlank(encodedId) ? encodedId : "")%>">
		<div class="panel"  style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
			<div class="panel-body" monitorTop="top" style="word-break: break-all">
				<div container="chart" style="height: 600px;width: 100%;"></div>
			</div>
		</div>
		<textarea name="monitorI18nInfo" rows="3" disabled="disabled" style="display:none;">
        <%
			StringBuilder keysBuilder = new StringBuilder();
			keysBuilder.append("{");
			boolean notFirst = false;
			for (String key : numericData.keySet()) {
				if (notFirst) keysBuilder.append(",");
				notFirst = true;

				String i18n = I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + key);
				keysBuilder.append("\"").append(key).append("\":[\"")
						.append(i18n).append("\",\"")
						.append(I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + key)).append("\"]");
			}
			keysBuilder.append("}");
			out.print(keysBuilder.toString());
		%>
        </textarea>

		<%
			Map<String, String> infoData = basicData;
		%>
		<%@ include file="../fragment/info.jsp" %>
	</div>

	<%@ include file="../fragment/back.jsp" %>
</div>
