<%@ page pageEncoding="UTF-8" %>

<%@ page import="java.util.*" %>
<%@ page import="java.util.stream.*" %>
<%@ page import="qingzhou.api.*" %>
<%@ page import="qingzhou.engine.util.*" %>
<%@ page import="qingzhou.api.type.*" %>
<%@ page import="java.util.List" %>
<%@ page import="qingzhou.console.*" %>
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
	String currentUser = LoginManager.getLoginUser(request);
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
%>

<%-- 初始化管理后的菜单页面 --%>
<%@ include file="../fragment/menu.jsp" %>

<section class="main-body" bindingId="<%=randBindingId%>">
	<%-- 面包屑分级导航 --%>
	<%@ include file="../fragment/breadcrumb.jsp" %>

	<%-- 首页面主体部分 --%>
	<div class="bodyDiv" bindingId="<%=randBindingId%>">
		<%
			Map<String, String> infoData = qzResponse.getDataMap();
		%>
		<%@ include file="../fragment/info.jsp" %>
	</div>
</section>
