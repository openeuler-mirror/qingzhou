<%@ page pageEncoding="UTF-8" %>

<%-- 左侧菜单 --%>
<aside class="main-sidebar" bindingId="<%=randBindingId%>">
	<div class="sidebar sidebar-scroll">
		<ul class="sidebar-menu" data-widget="tree">
			<%
				out.print(PageUtil.buildMenu(request, response, qzRequest));
			%>
		</ul>
	</div>

	<div class="menu-toggle-btn">
		<a href="javascript:void(0);" data-toggle="push-menu">
			<i class="icon icon-sliders"></i>
		</a>
	</div>
</aside>
