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
        readonly = " onclick='return false;' readonly";
    }
%>
<div class="checkbox-group sortable">
    <%
        String[] sortableCheckBoxOptions = PageBackendService.getFieldOptions(currentUser, qzApp, modelInfo.getCode(), fieldName);
        if(sortableCheckBoxOptions != null){
            List<String> list = Arrays.stream(sortableCheckBoxOptions).sorted(Comparator.comparingInt(fieldValues::indexOf)).collect(Collectors.toList());
            for (String option : list) {
        %>
        <a draggable="true" href="javascript:void(0);">
            <input type="checkbox" name="<%=fieldName%>"
                   value='<%=option%>' <%=(fieldValues.contains(option) ? "checked" : "")%> <%=readonly%>>
            <label><%=option%>
            </label>
        </a>
            <%
            }
        }
    %>
</div>
