<%@ page pageEncoding="UTF-8" %>
<%{
    LinkedHashMap<String, String> parentGroupDescriptions = new LinkedHashMap<>();
    LinkedHashMap<String, LinkedHashMap<String, String>> groupedOptions = new LinkedHashMap<>();
    PageUtil.groupMultiselectOptions(parentGroupDescriptions,groupedOptions,SystemController.getOptions(qzRequest, fieldName));
%>
<select <%=echoGroup%> name="<%=fieldName%>" multiple="multiple" style="width:100%;"
                       placeholder="<%=PageUtil.getPlaceholder(modelField, qzApp, qzModel, isForm)%>">
    <%
        for (Map.Entry<String, String> entry : parentGroupDescriptions.entrySet()) {
    %>
    <optgroup label="&nbsp;&nbsp;<%=entry.getValue()%>">
        <%
            LinkedHashMap<String, String> subMap = groupedOptions.get(entry.getKey());
            for (Map.Entry<String, String> en : subMap.entrySet()) {
                String option = en.getKey();
                String colorStyle = PageUtil.getColorStyle(modelInfo, fieldName, option);
        %>
        <option style="<%=colorStyle%>" value='<%=en.getKey()%>' <%=fieldValues.contains(en.getKey()) ? "selected" : ""%>>
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
<%}%>