<%@ page pageEncoding="UTF-8" %>

<%
    if (readonly == null) {
        return; // for 静态源码漏洞扫描
    }
    if (model == null) {
        return; // for 静态源码漏洞扫描
    }

    if (!"".equals(readonly)) {
        readonly = " onclick='return false;' readonly";
    }

    OptionManager radioOptions = ConsoleUtil.fieldOptions(qzRequest, fieldName);
    for (Option option : radioOptions.options()) {
        String val = option.value();
        String name = I18n.getString(option.i18n());
        %>
        <label class="radio-inline radio-label radio-anim">
            <input type="radio" name="<%=fieldName%>"
                   value='<%=val%>' <%=Objects.equals(fieldValue, val) ? "checked" : ""%> <%=readonly%>>
            <i class="radio-i"></i> <%=name%>
        </label>
        <%
    }
%>