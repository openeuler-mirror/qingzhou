<%@ page pageEncoding="UTF-8" %>

<%-- 面包屑分级导航 --%>
<ol class="breadcrumb" style="font-size: 15px; margin-bottom: 0px; min-width: 300px; padding: 10px 5px !important;">
    <li class="active" style="margin-left:-5px;">
        <div class="model-info">
            <span><%=I18n.getString(qzRequest.getAppName(), "model." + qzRequest.getModelName())%></span>
            <span class="tooltips" data-tip='<%=I18n.getString(qzRequest.getAppName(), "model.info." + qzRequest.getModelName())%>' data-tip-arrow="bottom-right" style="line-height:25px;">
				<i class="icon icon-question-sign"></i>
			</span>
        </div>
    </li>
    <%
        if (!Objects.equals(I18n.getString(qzRequest.getAppName(), "model.action." + qzRequest.getModelName() + "." + qzRequest.getModelName()), I18n.getString(qzRequest.getAppName(), "model." + qzRequest.getModelName()))) {
            if (StringUtil.notBlank(I18n.getString(qzRequest.getAppName(), "model.action." + qzRequest.getModelName() + "." + qzRequest.getActionName()))) {
                %>
                <li class="active">
                    <%=I18n.getString(qzRequest.getAppName(), "model.action." + qzRequest.getModelName() + "." + qzRequest.getActionName())%>
                </li>
                <%
            }
        }
    %>

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
</ol>
