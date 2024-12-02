<%@ page pageEncoding="UTF-8" %>

<select <%=echoGroup%> name="<%=fieldName%>" multiple="multiple" style="width:100%;"
                       placeholder="<%=isForm?modelField.getPlaceholder():(modelField.getPlaceholder().isEmpty()?I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName):modelField.getPlaceholder())%>">
    <%
        for (ItemInfo itemInfo : SystemController.getOptions(qzRequest, fieldName)) {
            String option = itemInfo.getName();
            String colorStyle = PageUtil.getColorStyle(modelInfo, fieldName, option);
    %>
    <option style="<%=colorStyle%>" value='<%=option%>' <%=fieldValues.contains(option) ? "selected" : ""%>>
        <%=I18n.getStringI18n(itemInfo.getI18n())%>
    </option>
    <%
        }
    %>
</select>