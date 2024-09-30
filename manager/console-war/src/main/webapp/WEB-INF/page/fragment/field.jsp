<%@ page import="java.util.function.Function" %>
<%@ page pageEncoding="UTF-8" %>

<%
    Map<String, List<String>> groupedFields = new LinkedHashMap<>();
    for (String fieldName : infoData.keySet()) {
        ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
        List<String> fields = groupedFields.computeIfAbsent(modelField.getGroup(), s -> new ArrayList<>());
        fields.add(fieldName);
    }
    boolean hasGroup = groupedFields.size() > 1 || Utils.notBlank(groupedFields.keySet().iterator().next());

    Double width = 99.6 / Math.min(3, groupedFields.size());
    GroupInfo[] groupInfos = modelInfo.getGroupInfos();
    int i = 0;
    for (String groupKey : groupedFields.keySet()) {
        GroupInfo gInfo = Arrays.stream(groupInfos).filter(groupInfo -> groupInfo.getName().equals(groupKey)).findAny().orElse(PageUtil.OTHER_GROUP);
        String panelClass = i % 2 == 0 ? "panel-success" : "panel-info";
        i++;
%>
<div class="panel <%=panelClass%>"
     style="border-radius: 2px; border-color:#EFEEEE;width: <%=width%>%;display: inline-block">
    <%
        if (hasGroup) {
    %>
    <div class="panel-heading"><%=I18n.getStringI18n(gInfo.getI18n())%>
    </div>
    <%
        }
    %>
    <div class="panel-body" style="word-break: break-all;">
        <table class="table home-table" style="margin-bottom: 1px;">
            <%
                for (String fieldName : groupedFields.get(groupKey)) {
                    ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);

                    String msg = I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName);
                    String info = "";
                    if (msg != null && !msg.startsWith("model.field.")) {
                        info = "<span class='tooltips' data-tip='" + msg + "'><i class='icon icon-question-sign' data-tip-arrow=\"right\"></i></span>";
                    }
                    String fieldValue = infoData.get(fieldName);
                    fieldValue = fieldValue != null ? fieldValue : "";
                    if (!FieldType.markdown.name().equals(modelField.getType())){
                        fieldValue = fieldValue.replace("\r\n", "<br>").replace("\n", "<br>").replace("<", "&lt;").replace(">", "&gt;");
                    }
            %>
            <tr row-item="<%=fieldName%>">
                <td class="home-field-info" field="<%=fieldName%>">
                    <label for="<%=fieldName%>"><%=info%>&nbsp;
                        <%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>
                    </label>
                </td>
                <td style="word-break: break-all" field-val="<%=fieldValue%>">
                    <%
                        if (fieldValue.startsWith("http") && !fieldValue.startsWith("http-")) {
                    %>
                    <a target="_blank" href="<%=fieldValue%>">
                        <%=PageUtil.styleFieldValue(fieldValue, modelField)%>
                    </a>
                    <%
                        } else {
                            if (FieldType.markdown.name().equals(modelField.getType())) {
                                out.print("<div class=\"markdownview\">"+ fieldValue +"</div>");
                            } else {
                                out.print(PageUtil.styleFieldValue(fieldValue, modelField));
                            }
                        }
                    %>
                </td>
            </tr>
            <%
                }
            %>
        </table>
    </div>
</div>
<%
    }
%>
<script>
    $(document).ready(function () {
        //为所有class=markdownview的div内容做转化
        const markdownViews = document.querySelectorAll('div.markdownview');

        // 使用 forEach 循环遍历每个元素
        markdownViews.forEach((element) => {
            // 在这里对每个元素进行操作
            let data = element.innerHTML;
            element.innerHTML = marked.parse(data);
        });
    });
</script>