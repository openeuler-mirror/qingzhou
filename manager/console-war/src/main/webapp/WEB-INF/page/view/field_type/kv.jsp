<%@ page pageEncoding="UTF-8" %>

<div class="form-control-kv">
	<input type="hidden" name="<%=fieldName%>" value='<%=fieldValue%>'>
	<table class="kv table table-bordered">
		<tr>
			<th style="padding:0px 0px !important; width: 30%; text-align: center; vertical-align: middle"><%=I18n.getKeyI18n("page.info.kv.name")%>
			</th>
			<th style="padding:0px 0px !important; text-align: center; vertical-align: middle"><%=I18n.getKeyI18n("page.info.kv.value")%>
			</th>
			<th style="width: 32px"></th>
		</tr>
		<%
			Map<String, String> map = Utils.stringToMap(fieldValue, DeployerConstants.DEFAULT_DATA_SEPARATOR);
			for (Map.Entry<String, String> entry : map.entrySet()) {
		%>
		<tr>
			<td class="edit-kv" style="padding:0px 0px !important; width: 30%;">
				<input type="text" class="form-control" value='<%=entry.getKey()%>'
					   onchange="refreshDict()"/>
			</td>
			<td class="edit-kv" style="padding:0px 0px !important;">
				<input type="text" class="form-control" value='<%=entry.getValue()%>'
					   onchange="refreshDict()"/>
			</td>
			<td class="narrow">
				<a href="javascript:void(0);" class="<%=!"".equals(readonly) ? "read-only" : ""%>"
				   onclick="removeDictRow(this, <%=!"".equals(readonly)%>);">
					<i class="icon icon-trash"></i>
				</a>
			</td>
		</tr>
		<%
			}
		%>
		<tr>
			<td colspan="3" style="text-align: center">
				<a href="javascript:void(0);" class="<%=!"".equals(readonly) ? "read-only" : ""%>"
				   onclick="addDictRow(this, <%=!"".equals(readonly)%>);">
					<i class="icon icon-plus" style="font-size:10px;"></i>
					<%=I18n.getKeyI18n("page.info.add")%>
				</a>
			</td>
		</tr>
	</table>
</div>
