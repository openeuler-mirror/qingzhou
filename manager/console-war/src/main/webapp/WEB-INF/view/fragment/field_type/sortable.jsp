<%@ page pageEncoding="UTF-8" %>
<div class="form-control-sortable" separator="<%=modelField.getSeparator()%>">
    <input type="hidden" name="<%=fieldName%>" value='<%=fieldValue%>'>
    <ul class="sortable">
        <%
            for (String val : fieldValues) {
        %>
        <li class="droptarget" draggable="true">
            <table class="table table-bordered">
                <tr>
                    <td class="editable" style="padding:0 !important;"><label><%= val%>
                    </label></td>
                    <td class="narrow">
                        <a href="javascript:void(0);" class="editable-edit"
                           onclick="bindEditable(this, false);">
                            <i class="icon icon-edit"></i>
                        </a>
                    </td>
                    <td class="narrow">
                        <a href="javascript:void(0);" onclick="removeRow(this, false);">
                            <i class="icon icon-trash"></i>
                        </a>
                    </td>
                    <td class="draggable narrow">
                        <a href="javascript:void(0);" draggable="true">
                            <i class="icon icon-arrows"></i>
                        </a>
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
            <a href="javascript:void(0);" onclick="addRow(this, false);">
                <i class="icon icon-plus" style="font-size:10px;"></i>
                <%=I18n.getKeyI18n("page.info.add")%>
            </a>
        </li>
    </ul>
</div>
