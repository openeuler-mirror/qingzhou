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
    Options multiOptionManager = modelManager.getOptions(qzRequest, qzRequest.getModelName(), fieldName);
%>
<select name="<%=fieldName%>" multiple="multiple" <%=readonly%> style="width:100%;">
    <%
        if (multiOptionManager != null) {
            for (Option option : multiOptionManager.options()) {
    %>
    <option value='<%=option.value()%>' <%=fieldValues.contains(option.value()) ? "selected" : ""%>>
        <%=I18n.getString(option.i18n())%>
    </option>
    <%
            }
        }
    %>
</select>