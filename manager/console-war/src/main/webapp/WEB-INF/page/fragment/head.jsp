<%@ page pageEncoding="UTF-8" %>

<%@ page import="qingzhou.api.FieldType" %>
<%@ page import="qingzhou.api.Request" %>
<%@ page import="qingzhou.api.Response" %>
<%@ page import="qingzhou.api.type.*" %>
<%@ page import="qingzhou.console.SecurityController" %>
<%@ page import="qingzhou.console.controller.I18n" %>
<%@ page import="qingzhou.console.controller.SystemController" %>
<%@ page import="qingzhou.console.controller.Theme" %>
<%@ page import="qingzhou.console.controller.rest.RESTController" %>
<%@ page import="qingzhou.console.login.LoginManager" %>
<%@ page import="qingzhou.console.page.PageBackendService" %>
<%@ page import="qingzhou.console.view.ViewManager" %>
<%@ page import="qingzhou.deployer.DeployerConstants" %>
<%@ page import="qingzhou.deployer.RequestImpl" %>
<%@ page import="qingzhou.engine.util.Utils" %>
<%@ page import="qingzhou.registry.ModelActionInfo" %>
<%@ page import="qingzhou.registry.ModelFieldInfo" %>
<%@ page import="qingzhou.registry.ModelInfo" %>
<%@ page import="java.util.*" %>

<%
    String currentUser = LoginManager.getLoginUser(request);
    RequestImpl qzRequest = (RequestImpl) request.getAttribute(Request.class.getName());
    String qzApp = SystemController.getAppName(qzRequest);
    String qzModel = qzRequest.getModel();
    String qzAction = qzRequest.getAction();
    ModelInfo modelInfo = qzRequest.getCachedModelInfo();
    String id = qzRequest.getId();
    String encodedId = RESTController.encodeId(id);
    Response qzResponse = qzRequest.getResponse();
    String themeMode = (String) session.getAttribute(Theme.KEY_THEME_MODE);
%>

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
