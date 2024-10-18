<%@ page pageEncoding="UTF-8" %>

<div class="block-bg">
    <%
        List<Map<String, String>> dataList = qzResponse.getDataList();
        Map<String, String> infoData;
        if (dataList == null || dataList.isEmpty()) {
            infoData = new HashMap<>();
            System.out.println("The data should not be empty !!!");
        } else {
            infoData = dataList.get(0);
        }
    %>
    <%@ include file="field.jsp" %>
</div>