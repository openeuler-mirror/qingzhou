<%@ page pageEncoding="UTF-8" %>

<select <%=echoGroup%> name="<%=fieldName%>" multiple="multiple" style="width:100%;"
                       placeholder="<%=PageUtil.getPlaceholder(modelField, qzApp, qzModel, isForm)%>">
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