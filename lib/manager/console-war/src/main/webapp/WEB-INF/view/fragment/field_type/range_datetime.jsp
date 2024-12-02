<%@ page pageEncoding="UTF-8" %>
<%
    {
        String tempId = java.util.UUID.randomUUID().toString().replaceAll("-", "");
        String fieldValueForTemp = "";
        //少定义变量，防止多个冲突
        try {
            fieldValueForTemp = new SimpleDateFormat(DeployerConstants.DATETIME_FORMAT).format(new Date(Long.parseLong(fieldValue.split(modelField.getSeparator())[0])))
                    + modelField.getSeparator()
                    + new SimpleDateFormat(DeployerConstants.DATETIME_FORMAT).format(new Date(Long.parseLong(fieldValue.split(modelField.getSeparator())[1])));
        } catch (Exception exception) {
            //转化异常表示是从request里面拿的fieldValue，此时不用转化
        }
%>
<input type="text" name="<%=fieldName%>" value='<%=fieldValueForTemp%>'
       placeholder="<%=isForm?modelField.getPlaceholder():(modelField.getPlaceholder().isEmpty()?I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName):modelField.getPlaceholder())%>"
       id="<%=tempId%>"
       class="form-control"/>
<script type="text/javascript">
    $(function () {
        $('#<%=tempId%>').daterangepicker({
            locale: {
                format: 'yyyy-MM-DD HH:mm:ss',
                separator: '<%=modelField.getSeparator()%>',          // 范围分隔符
                <%
                   if (I18n.isZH()) {
                %>
                applyLabel: '确认',
                cancelLabel: '取消',
                daysOfWeek: ['日', '一', '二', '三', '四', '五', '六'],
                monthNames: ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'],
                <%
                }
                %>
            },
            opens: 'left',                 // 选择器显示方向
            drops: 'auto',
            timePicker: true,               // 启用时间选择
            timePickerIncrement: 1,         // 每次增量选择的分钟数
            timePicker24Hour: true,        // 24小时制
            timePickerSeconds: true,       //启用秒
        }).on('cancel.daterangepicker', function(ev, picker) {
            // 清空输入框
            $(this).val('');
        });
        <%
            if (fieldValueForTemp.isEmpty()){
        %>
        $('#<%=tempId%>').val("")
        <%
            }
        %>
    });
</script>
<%
    }
%>