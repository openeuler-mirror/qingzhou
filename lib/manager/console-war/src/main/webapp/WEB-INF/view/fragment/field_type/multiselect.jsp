<%@ page pageEncoding="UTF-8" %>

<select <%=echoGroup%> name="<%=fieldName%>" multiple="multiple" style="width:100%;"
                       placeholder="<%=PageUtil.getPlaceholder(modelField, qzApp, qzModel, isForm)%>">
    <%
        for (ItemData itemData : SystemController.getOptions(qzRequest, fieldName)) {
            String option = itemData.getName();
            String colorStyle = PageUtil.getColorStyle(modelInfo, fieldName, option);
    %>
    <option style="<%=colorStyle%>" value='<%=option%>' <%=fieldValues.contains(option) ? "selected" : ""%>>
        <%=I18n.getStringI18n(itemData.getI18n())%>
    </option>
    <%
        }
    %>
</select>