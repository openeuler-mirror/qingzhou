<%@ page pageEncoding="UTF-8" %>

<%@ page import="java.util.*" %>
<%@ page import="java.util.stream.*" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="qingzhou.api.*" %>
<%@ page import="qingzhou.engine.util.*" %>
<%@ page import="qingzhou.api.type.*" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.function.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="qingzhou.console.*" %>
<%@ page import="qingzhou.console.controller.*" %>
<%@ page import="qingzhou.console.controller.rest.*" %>
<%@ page import="qingzhou.console.login.*" %>
<%@ page import="qingzhou.console.view.*" %>
<%@ page import="qingzhou.console.view.type.*" %>
<%@ page import="qingzhou.console.page.*" %>
<%@ page import="qingzhou.core.*" %>
<%@ page import="qingzhou.core.registry.*" %>
<%@ page import="qingzhou.core.deployer.*" %>
<%@ page import="qingzhou.core.deployer.RequestImpl" %>
<%@ page import="qingzhou.core.deployer.ResponseImpl" %>
<%@ page import="qingzhou.core.registry.ModelInfo" %>

<%
    RequestImpl qzRequest = (RequestImpl) request.getAttribute(Request.class.getName());
    String qzApp = qzRequest.getApp();
    String qzModel = qzRequest.getModel();
    String qzAction = qzRequest.getAction();
    ModelInfo modelInfo = qzRequest.getCachedModelInfo();
    String id = qzRequest.getId();
    String encodedId = RESTController.encodeId(id);
    ResponseImpl qzResponse = (ResponseImpl) qzRequest.getResponse();
    String themeMode = (String) session.getAttribute(Theme.KEY_THEME_MODE);
    String randBindingId = java.util.UUID.randomUUID().toString();
    boolean isForm = Objects.equals(Update.ACTION_EDIT, qzAction) || Objects.equals(Add.ACTION_CREATE, qzAction);
%>
