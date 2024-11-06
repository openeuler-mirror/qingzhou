<%@ page pageEncoding="UTF-8" %>

<select name="<%=fieldName%>" multiple="multiple" style="width:100%;" placeholder="<%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>">
    <%
        for (ItemInfo itemInfo : SystemController.getOptions(qzApp, modelInfo, fieldName)) {
            String option = itemInfo.getName();
            String[] color = SystemController.getColor(modelInfo, fieldName);
            String colorStyle = "";
            if (color != null){
                for (String condition : color) {
                    String[] array = condition.split(":");
                    if (array.length != 2) {
                        continue;
                    }
                    if (array[0].equals(option)) {
                        colorStyle = "color:" + array[1];
                        break;
                    }
                }
            }
    %>
    <option style="<%=colorStyle%>" value='<%=option%>' <%=fieldValues.contains(option) ? "selected" : ""%>>
        <%=I18n.getStringI18n(itemInfo.getI18n())%>
    </option>
    <%
        }
    %>
</select>