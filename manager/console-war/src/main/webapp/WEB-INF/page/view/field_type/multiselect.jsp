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

    if (!readonly.isEmpty()) {
        readonly = "onfocus='this.defaultIndex=this.selectedIndex;' onchange='this.selectedIndex=this.defaultIndex;' readonly";
    }
    String[] multiOptions = PageBackendService.getFieldOptions(menuAppName, modelInfo.getCode(), fieldName);
%>
<select name="<%=fieldName%>" multiple="multiple" <%=readonly%> style="width:100%;">
    <%
        if (multiOptions != null) {
            for (String option : multiOptions) {
    %>
    <option value='<%=option%>' <%=fieldValues.contains(option) ? "selected" : ""%>>
        <%=option%>
    </option>
    <%
            }
        }
    %>
</select>