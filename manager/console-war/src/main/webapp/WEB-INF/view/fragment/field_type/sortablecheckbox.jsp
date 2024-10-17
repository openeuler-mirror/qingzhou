<%@ page pageEncoding="UTF-8" %>

<%
    // 获取所有选项并放入 map
    Map<String, ItemInfo> optionMap = new LinkedHashMap<>();
    for (ItemInfo itemInfo : SystemController.getOptions(qzApp, modelInfo, fieldName)) {
        optionMap.put(itemInfo.getName(), itemInfo);
    }
%>
<div class="checkbox-group sortable">
    <%
        // 循环已选的值，生成已勾选的 checkbox
        for (String field : fieldValues) {
            ItemInfo selectedItem = optionMap.get(field);
            if (selectedItem != null) {
    %>
    <a draggable="true" href="javascript:void(0);">
        <input checked type="checkbox" name="<%=fieldName%>" value="<%=field%>"/>
        <label><%=I18n.getStringI18n(selectedItem.getI18n())%>
        </label>
    </a>
    <%
                // 移除已选项，避免重复输出
                optionMap.remove(field);
            }
        }

        // 输出未选中的 checkbox
        for (ItemInfo itemInfo : optionMap.values()) {
    %>
    <a draggable="true" href="javascript:void(0);">
        <input type="checkbox" name="<%=fieldName%>" value="<%=itemInfo.getName()%>"/>
        <label><%=I18n.getStringI18n(itemInfo.getI18n())%>
        </label>
    </a>
    <%
        }
    %>
</div>