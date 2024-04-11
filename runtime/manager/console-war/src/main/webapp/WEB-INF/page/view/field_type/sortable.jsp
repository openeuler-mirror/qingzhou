<%@ page pageEncoding="UTF-8" %>

<%
    if (readonly == null) {
        return; // for 静态源码漏洞扫描
    }
    if (fieldValue == null) {
        return; // for 静态源码漏洞扫描
    }
%>

<div class="form-control-sortable">
    <input type="hidden" name="<%=fieldName%>" value='<%=fieldValue%>'>
    <ul class="sortable">
        <%
            String[] vals = (fieldValue == null ? "" : fieldValue).split(Validator.DATA_SEPARATOR);
            for (int ii = 0; ii < vals.length; ii++) {
        %>
        <li class="droptarget" draggable="true">
            <table class="table table-bordered">
                <tr>
                    <td class="editable" style="padding:0px 0px !important;"><label><%= vals[ii]%>
                    </label></td>
                    <td class="narrow">
                        <a href="javascript:void(0);" class="editable-edit <%=!"".equals(readonly) ? "read-only" : ""%>"
                           onclick="bindEditable(this, <%=!"".equals(readonly)%>);">
                            <i class="icon icon-edit"></i>
                        </a>
                    </td>
                    <td class="narrow">
                        <a href="javascript:void(0);" class="<%=!"".equals(readonly) ? "read-only" : ""%>"
                           onclick="removeRow(this, <%=!"".equals(readonly)%>);">
                            <i class="icon icon-trash"></i>
                        </a>
                    </td>
                    <td class="draggable narrow">
                        <a href="javascript:void(0);" draggable="true"><i class="icon icon-arrows"></i></a>
                    </td>
                </tr>
            </table>
        </li>
        <%
            }
        %>
    </ul>
    <ul class="sortable-add">
        <li>
            <a href="javascript:void(0);" class="<%=!"".equals(readonly) ? "read-only" : ""%>"
               onclick="addRow(this, <%=!"".equals(readonly)%>);">
                <i class="icon icon-plus" style="font-size:10px;"></i>
                <%=PageBackendService.getMasterAppI18nString("page.info.add")%>
            </a>
        </li>
    </ul>
</div>
