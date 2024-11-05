<%@ page pageEncoding="UTF-8" %>

<%@include file="common.jsp" %>

<script type="text/javascript">
    $(document).ready(function () {
        // 新页面，首先关闭之前的通知消息弹窗
        try {
            $("#" + $(getActiveTabContent()).attr("showInfoIndex")).remove();
            closeLayer($(getActiveTabContent()).attr("showInfoIndex"));
        } catch (e) {
            // login.jsp
        }
    });
</script>

<%--公用“重定向页面”消息提示--%>
<%
    String common_msg = request.getParameter(RESTController.MSG_FLAG);
    common_msg = LoginManager.retrieveI18nMsg(common_msg);
    if (common_msg != null) {
%>
<script>
    $(document).ready(function () {
        window.setTimeout(function () {
            var common_msgIndex = showMsg("<%=common_msg%>", "error");
            // 记录最后一次通知弹窗
            try {
                $(getActiveTabContent()).attr("showInfoIndex", common_msgIndex);
            } catch (e) {
                // login.jsp
            }
        }, 350);
    });
</script>
<%
    }
%>

<%
    if (!qzResponse.isSuccess()) {
%>
<script type="text/javascript">
    $(document).ready(function () {
        var forward_msgIndex = showMsg("<%=qzResponse.getMsg()%>", "error");
        // 记录最后一次通知弹窗
        try {
            $(getActiveTabContent()).attr("showInfoIndex", forward_msgIndex);
        } catch (e) {
            // login.jsp
        }
    });
</script>
<%
    }
%>
