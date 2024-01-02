<%@ page pageEncoding="UTF-8" %>

<%@ page import="qingzhou.framework.api.*" %>
<%@ page import="qingzhou.console.ConsoleUtil" %>
<%@ page import="qingzhou.console.controller.rest.*" %>
<%@ page import="qingzhou.console.controller.system.*" %>
<%@ page import="qingzhou.console.login.LoginManager" %>
<%@ page import="qingzhou.console.login.vercode.*" %>
<%@ page import="qingzhou.console.login.*" %>
<%@ page import="qingzhou.console.view.ViewManager" %>
<%@ page import="qingzhou.console.view.impl.*" %>
<%@ page import="qingzhou.framework.api.Constants" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.*" %>
<%@ page import="qingzhou.console.ServerXml" %>
<%@ page import="qingzhou.framework.console.Lang" %>
<%@ page import="qingzhou.framework.console.I18n" %>
<%@ page import="qingzhou.console.controller.rest.*" %>
<%@ page import="qingzhou.console.SecureKey" %>
<%@ page import="qingzhou.framework.util.*" %>

<%
    String currentUser = LoginManager.getLoginUser(session);
    Request qzRequest = (Request) request.getAttribute(HtmlView.QZ_REQUEST_KEY);
    Response qzResponse = (Response) request.getAttribute(HtmlView.QZ_RESPONSE_KEY);
    String initAppName = qzRequest == null ? Constants.MASTER_APP_NAME : qzRequest.getAppName();
    ModelManager modelManager = ConsoleUtil.getModelManager(initAppName);
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
        if (!SafeCheckerUtil.checkIsXSS(common_msg)) {
%>
<script>
    $(document).ready(function () {
        var common_msgIndex = showError("<%=common_msg%>");
        // 记录最后一次通知弹窗
        try {
            $(getActiveTabContent()).attr("showInfoIndex", common_msgIndex);
        } catch (e) {
            // login.jsp
        }
    });
</script>
<%
        }
    }
%>

<%--公用“转发”错误提示：三员密码必需本机修改的提示--%>
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

<%--公用“通知”消息提示--%>
<%
    List<Map<String, String>> noticeModes = StringUtil.isBlank(currentUser) ? new ArrayList<>() : ConsoleUtil.listModels(request, TargetType.node.name(), Constants.LOCAL_NODE_NAME, Constants.MASTER_APP_NAME, "notice");
    StringBuilder noticeBuilder = new StringBuilder();
    for (int jj = 0; jj < noticeModes.size(); jj++) {
        Map<String, String> mb = noticeModes.get(jj);
        String msg = mb.get("msg");
        String detail = mb.get("detail");
        noticeBuilder.append(I18n.getString(initAppName, "model." + mb.get("modelName"))).append("：").append(detail);
        if (jj != noticeModes.size() - 1) {
            noticeBuilder.append("<br>");
        }
    }
    if (AccessControl.canAccess(qzRequest!=null?qzRequest.getTargetType():TargetType.node.name(), Constants.MASTER_APP_NAME +"/notice/" + ListModel.ACTION_NAME_LIST, LoginManager.getLoginUser(session))) {
        int noticeSize = noticeModes.size();
%>
<script type="text/javascript">
    var noticeLayerIdx = 0;
    var timerLimit = 20;
    window.setTimeout(function fn() {
        if ($("span.noticeNumber", $(".tab-box>ul>li.active")).length > 0) {
            $("span.noticeNumber", $(".tab-box>ul>li.active")).toggle(<%=noticeSize > 0%>).html("<%=noticeSize%>").hover(function () {
                noticeLayerIdx = layer.tips('<%=noticeBuilder.toString()%>', this, {
                    anim: -1,
                    isOutAnim: false,
                    tips: [2, '#2F4056'],
                    area: 'auto',
                    maxWidth: '1000',
                    time: 0
                });
            }, function () {
                layer.close(noticeLayerIdx);
            });
        } else {
            if (timerLimit-- > 0) {
                window.setTimeout(fn, 50);
            }
        }
    }, 50);
</script>
<%
    }

// 当前模块的“通知”消息
    if (!noticeModes.isEmpty()) {
        for (Map<String, String> mb : noticeModes) {
            String modelName = mb.get("modelName");
            String msg = mb.get("msg");
            if (qzRequest != null && qzRequest.getModelName().equals(modelName)) {
%>
<script type="text/javascript">
    $(document).ready(function () {
        var noticeIndex = showInfo("<%=(I18n.getString(initAppName, "model." + qzRequest.getModelName()) + ": " + msg)%> | <%=(I18n.getString(Constants.MASTER_APP_NAME,"page.go"))%> <%=(I18n.getString(Constants.MASTER_APP_NAME, "model.notice"))%>");
        // 记录最后一次通知弹窗
        try {
            $(getActiveTabContent()).attr("showInfoIndex", noticeIndex);
        } catch (e) {
            // login.jsp
        }
    });
</script>
<%
                break;
            }
        }
    }
%>
