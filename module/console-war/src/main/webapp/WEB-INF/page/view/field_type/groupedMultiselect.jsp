<%@ page pageEncoding="UTF-8" %>

<%
if (readonly == null) {
    return; // for 静态源码漏洞扫描
}
if (model == null) {
    return; // for 静态源码漏洞扫描
}
if (fieldValues == null) {
    return; // for 静态源码漏洞扫描
}

if (!"".equals(readonly)) {
    readonly = "onfocus='this.defaultIndex=this.selectedIndex;' onchange='this.selectedIndex=this.defaultIndex;' readonly";
}
LinkedHashMap<String, String> groupMultiDes = new LinkedHashMap<>();
LinkedHashMap<String, LinkedHashMap<String, String>> groupedMultiMap = new LinkedHashMap<>();
Options multiselectOptionManager = modelManager.getOptions(qzRequest.getModelName(), fieldName);
PageBackendService.multiSelectGroup(groupMultiDes, groupedMultiMap, multiselectOptionManager);
%>
<select name="<%=fieldName%>" multiple="multiple" <%=readonly%> style="width:100%;">
    <%
    for (Map.Entry<String, String> entry : groupMultiDes.entrySet()) {
        %>
        <optgroup label="&nbsp;&nbsp;<%=entry.getValue()%>">
        <%
            LinkedHashMap<String, String> subMap = groupedMultiMap.get(entry.getKey());
            for (Map.Entry<String, String> en : subMap.entrySet()) {
                 %>
                <option value='<%=en.getKey()%>' <%=fieldValues.contains(en.getKey()) ? "selected" : ""%>>
                    <%=en.getValue()%>
                </option>
                <%
            }
        %>
        </optgroup>
        <%
    }
    %>
</select>