<%@ page pageEncoding="UTF-8" %>

<%-- 面包屑分级导航 --%>
<ol class="breadcrumb" style="font-size: 15px; margin-bottom: 0; min-width: 300px; padding: 10px 5px !important;">
    <li class="active" style="margin-left:-5px;">
        <div class="model-info">
            <span><%=I18n.getString(menuAppName, "model." + qzRequest.getModel())%></span>
            <span class="tooltips" data-tip='<%=I18n.getString(menuAppName, "model.info." + qzRequest.getModel())%>'
                  data-tip-arrow="bottom-right" style="line-height:25px;">
                <i class="icon icon-question-sign"></i>
            </span>
        </div>
    </li>
    <%
        if (!Objects.equals(I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + qzRequest.getModel()), I18n.getString(menuAppName, "model." + qzRequest.getModel()))) {
            if (I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + qzRequest.getAction()) != null) {
    %>
    <li class="active">
        <%=I18n.getString(menuAppName, "model.action." + qzRequest.getModel() + "." + qzRequest.getAction())%>
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
                if (data.success) {
                    showSuccess(data.message, function () {
                        window.location.href = window.location.href;
                    });
                } else {
                    showError(data.message);
                }
            }
        });
    }
</script>
