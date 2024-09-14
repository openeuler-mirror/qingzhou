<%@ page pageEncoding="UTF-8" %>

<%-- 面包屑分级导航 --%>
<ol class="breadcrumb" style="font-size: 15px; margin-bottom: 0; min-width: 300px; padding: 10px 5px !important;">
    <li class="active" style="margin-left:-5px;">
        <div class="model-info">
            <span><%=I18n.getModelI18n(qzApp, "model." + qzModel)%></span>
            <span class="tooltips" data-tip='<%=I18n.getModelI18n(qzApp, "model.info." + qzModel)%>'
                  data-tip-arrow="bottom-right" style="line-height:25px;">
                <i class="icon icon-question-sign"></i>
            </span>
        </div>
    </li>
    <%
        if (!Objects.equals(I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + qzAction), I18n.getModelI18n(qzApp, "model." + qzModel))) {
            if (I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + qzAction) != null) {
    %>
    <li class="active">
        <%=I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + qzAction)%>
    </li>
    <%
            }
        }
    %>
</ol>

<script type="text/javascript">
    function favoirtes(url) {
        $.ajax({
            url: url,
            type: "post",
            async: false,
            success: function (data) {
                showMsg(data.message, data.msg_level, function () {
                    window.location.href = window.location.href;
                })
            }
        });
    }
</script>
