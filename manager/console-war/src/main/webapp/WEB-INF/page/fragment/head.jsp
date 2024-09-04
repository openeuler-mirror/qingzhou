<%@ page pageEncoding="UTF-8" %>

<%@ page import="java.util.*" %>
<%@ page import="java.util.stream.*" %>
<%@ page import="qingzhou.api.*" %>
<%@ page import="qingzhou.api.type.*" %>
<%@ page import="qingzhou.console.*" %>
<%@ page import="qingzhou.console.i18n.*" %>
<%@ page import="qingzhou.console.controller.*" %>
<%@ page import="qingzhou.console.controller.rest.*" %>
<%@ page import="qingzhou.console.login.*" %>
<%@ page import="qingzhou.console.login.vercode.*" %>
<%@ page import="qingzhou.console.view.*" %>
<%@ page import="qingzhou.console.view.type.*" %>
<%@ page import="qingzhou.console.page.*" %>
<%@ page import="qingzhou.registry.*" %>
<%@ page import="qingzhou.deployer.*" %>

<%
    String currentUser = LoginManager.getLoginUser(session);
    Request qzRequest = (Request) request.getAttribute(Request.class.getName());
    String qzApp = PageBackendService.getAppName(qzRequest);
    String qzModel = qzRequest.getModel();
    String qzAction = qzRequest.getAction();
    AppInfo appInfo = SystemController.getAppInfo(qzApp);
    ModelInfo modelInfo = PageBackendService.getModelInfo(qzRequest);
    String idFieldName = modelInfo.getIdFieldName();
    Response qzResponse = qzRequest.getResponse();
    String themeMode = session == null ? "" : (String)session.getAttribute(DeployerConstants.KEY_THEME_MODE);
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
            var common_msgIndex = showError("<%=common_msg%>");
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
    if (qzResponse != null && !qzResponse.isSuccess()) {
%>
<script type="text/javascript">
    $(document).ready(function () {
        var forward_msgIndex = showError("<%=qzResponse.getMsg()%>");
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
