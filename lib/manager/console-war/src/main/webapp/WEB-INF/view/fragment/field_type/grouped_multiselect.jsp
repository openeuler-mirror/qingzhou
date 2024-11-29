<%@ page pageEncoding="UTF-8" %>
<%{
    LinkedHashMap<String, String> parentGroupDescriptions = new LinkedHashMap<>();
    LinkedHashMap<String, LinkedHashMap<String, String>> groupedOptions = new LinkedHashMap<>();
    SystemController.groupMultiselectOptions(parentGroupDescriptions,groupedOptions,SystemController.getOptions(qzRequest, fieldName));
%>
<select <%=echoGroup%> name="<%=fieldName%>" multiple="multiple" style="width:100%;"
                       placeholder="<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>">
    <%
        for (Map.Entry<String, String> entry : parentGroupDescriptions.entrySet()) {
    %>
    <optgroup label="&nbsp;&nbsp;<%=entry.getValue()%>">
        <%
            LinkedHashMap<String, String> subMap = groupedOptions.get(entry.getKey());
            for (Map.Entry<String, String> en : subMap.entrySet()) {
                String option = en.getKey();
                String colorStyle = SystemController.getColorStyle(modelInfo, fieldName, option);
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