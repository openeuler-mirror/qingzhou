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
<div class="checkbox-group sortable">
    <%
    OptionManager manager = ConsoleUtil.fieldOptions(qzRequest, fieldName);
    List<Option> list = manager.options();
    Collections.sort(list, Comparator.comparingInt(o -> fieldValues.indexOf(o.value())));
    for (Option option : list) {
        String val = option.value();
        String name = I18n.getString(option.i18n());
        %>
        <a draggable="true" href="javascript:void(0);">
            <input type="checkbox" name="<%=fieldName%>"
                   value='<%=val%>' <%=(fieldValues.contains(val) ? "checked" : "")%> <%=readonly%>>
            <label><%=name%>
            </label>
        </a>
        <%
    }
    %>
</div>
