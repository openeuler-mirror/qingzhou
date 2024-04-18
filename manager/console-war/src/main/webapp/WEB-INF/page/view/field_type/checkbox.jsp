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

    if (!"".equals(readonly)) {
        readonly = " onclick='return false;' readonly";
    }
%>
<%
    {
        Options checkboxOptions = modelManager.getOptions(qzRequest, qzRequest.getModel(), fieldName);
        if (checkboxOptions != null) {
            for (Option option : checkboxOptions.options()) {
                String val = option.value();
%>
<label class="checkbox-inline checkbox-label checkbox-anim">
    <input type="checkbox" name="<%=fieldName%>"
           value='<%=val%>' <%=(fieldValues.contains(val) ? "checked" : "")%> <%=readonly%>>
    <i class="checkbox-i"></i> <%=I18n.getString(option.i18n())%>
</label>
<%
            }
        }
    }
%>