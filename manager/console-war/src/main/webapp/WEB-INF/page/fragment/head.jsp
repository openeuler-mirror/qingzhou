<%@ page pageEncoding="UTF-8" %>

<%@ page import="java.util.*" %>
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
<%@ page import="qingzhou.registry.ModelInfo" %>
<%@ page import="qingzhou.registry.AppInfo" %>
<%@ page import="qingzhou.registry.ModelFieldInfo" %>
<%@ page import="qingzhou.deployer.ResponseImpl" %>
<%@ page import="qingzhou.deployer.RequestImpl" %>

<%
    String currentUser = LoginManager.getLoginUser(session);
    RequestImpl qzRequest = (RequestImpl) request.getAttribute(HtmlView.QZ_REQUEST_KEY);
    ResponseImpl qzResponse = (ResponseImpl) request.getAttribute(HtmlView.QZ_RESPONSE_KEY);
    String menuAppName = PageBackendService.getAppName(qzRequest);
    AppInfo appInfo = PageBackendService.getAppInfo(menuAppName);
    ModelInfo modelInfo = PageBackendService.getModelInfo(qzRequest);
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
