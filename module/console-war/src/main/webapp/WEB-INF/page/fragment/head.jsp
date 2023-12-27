<%@ page pageEncoding="UTF-8" %>

<%@ page import="qingzhou.api.console.FieldType" %>

<%@ page import="qingzhou.api.console.Model" %>
<%@ page import="qingzhou.api.console.ModelAction" %>
<%@ page import="qingzhou.api.console.ModelField" %>
<%@ page import="qingzhou.api.console.ModelManager" %>
<%@ page import="qingzhou.api.console.group.GroupManager" %>

<%@ page import="qingzhou.api.console.MonitoringField" %>
<%@ page import="qingzhou.api.console.model.AddModel" %>
<%@ page import="qingzhou.api.console.model.DownloadModel" %>
<%@ page import="qingzhou.api.console.model.EditModel" %>
<%@ page import="qingzhou.api.console.model.ListModel" %>
<%@ page import="qingzhou.api.console.model.ModelBase" %>
<%@ page import="qingzhou.api.console.model.MonitorModel" %>
<%@ page import="qingzhou.api.console.model.ShowModel" %>
<%@ page import="qingzhou.api.console.option.Option" %>
<%@ page import="qingzhou.api.console.option.OptionManager" %>
<%@ page import="qingzhou.console.ConsoleUtil" %>
<%@ page import="qingzhou.console.auth.AccessControl" %>
<%@ page import="qingzhou.console.controller.RESTController" %>
<%@ page import="qingzhou.console.i18n.I18nFilter" %>
<%@ page import="qingzhou.console.login.LoginManager" %>
<%@ page import="qingzhou.console.login.vercode.VerCode" %>
<%@ page import="qingzhou.console.sdk.ConsoleSDK" %>
<%@ page import="qingzhou.console.view.ViewManager" %>
<%@ page import="qingzhou.console.view.impl.HtmlView" %>
<%@ page import="qingzhou.console.util.Constants" %>
<%@ page import="qingzhou.console.util.SafeCheckerUtil" %>
<%@ page import="qingzhou.console.util.ServerUtil" %>
<%@ page import="qingzhou.console.util.StringUtil" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Objects" %>
<%@ page import="java.util.Properties" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="qingzhou.console.RequestImpl" %>
<%@ page import="qingzhou.api.console.data.Response" %>
<%@ page import="qingzhou.console.ResponseImpl" %>
<%@ page import="qingzhou.console.util.Constants" %>
<%@ page import="qingzhou.console.util.SafeCheckerUtil" %>
<%@ page import="qingzhou.console.util.StringUtil" %>
<%@ page import="qingzhou.console.ServerXml" %>
<%@ page import="qingzhou.framework.app.Lang" %>
<%@ page import="qingzhou.framework.app.I18n" %>

<%
    String currentUser = LoginManager.getLoginUser(session);
    RequestImpl qzRequest = (RequestImpl) request.getAttribute(HtmlView.QZ_REQUEST_KEY);
    Response qzResponse = (ResponseImpl) request.getAttribute(HtmlView.QZ_RESPONSE_KEY);
    String initAppName = qzRequest == null ? Constants.QINGZHOU_MASTER_APP_NAME : ConsoleUtil.getAppName(qzRequest.getTargetType(), qzRequest.getTargetName());
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
    List<Map<String, String>> noticeModes = StringUtil.isBlank(currentUser) ? new ArrayList<>() : ConsoleUtil.listModels(request, Constants.MODEL_NAME_instance, Constants.QINGZHOU_DEFAULT_APP_NAME, Constants.MODEL_NAME_notice);
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
    if (AccessControl.canAccess(qzRequest!=null?qzRequest.getTargetType():Constants.MODEL_NAME_instance, Constants.QINGZHOU_DEFAULT_APP_NAME +"/"+ Constants.MODEL_NAME_notice + "/" + ListModel.ACTION_NAME_LIST, LoginManager.getLoginUser(session))) {
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
        var noticeIndex = showInfo("<%=(I18n.getString(initAppName, "model." + qzRequest.getModelName()) + ": " + msg)%> | <%=(I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME,"page.go"))%> <%=(I18n.getString(Constants.QINGZHOU_DEFAULT_APP_NAME, "model." + Constants.MODEL_NAME_notice))%>");
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
