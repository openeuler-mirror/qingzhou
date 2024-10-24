<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <table class="table table-striped table-hover">
        <tr>
            <td>
                <%=I18n.getKeyI18n("page.status")%>:
            </td>
            <td>
                <%=qzResponse.isSuccess()%>
            </td>
        </tr>
        <tr>
            <td>
                <%=I18n.getKeyI18n("page.msg")%>:
            </td>
            <td>
                <%=qzResponse.getMsg()%>
            </td>
        </tr>
        <tr>
            <td>
            </td>
            <td>
                <%
                    for (Map<String, String> data : qzResponse.getDataList()) {
                        for (Map.Entry<String, String> e : data.entrySet()) {
                            String key = e.getKey();
                            String value = e.getValue();
                %>
                <p>
						<span class="key">
						<%
                            if (key.startsWith("http")) {
                        %>
							<a target="_blank" href="<%=key%>"><%=key%></a>
						<%
                            } else {
                                out.print(key);
                            }
                        %>
						</span>
                    <span class="value">
						<%
                            if (!value.isEmpty()) {
                                if (value.startsWith("http")) {
                        %>
									<a target="_blank" href="<%=value%>"><%=value%></a>
									<%
                                            } else {
                                                out.print(value);
                                            }
                                        }
                                    %>
						</span>
                </p>
                <%
                        }
                    }
                %>
            </td>
        </tr>
    </table>

    <%
        if (SecurityController.isActionPermitted(qzApp, qzModel, qingzhou.api.type.List.ACTION_LIST, currentUser)) {
    %>
    <div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
        <div class="form-btn">
            <a class="btn" onclick="returnHref('<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, qingzhou.api.type.List.ACTION_LIST)%>')"
               href="javascript:void(0)">
                    <%=I18n.getKeyI18n("page.return")%></a>
        </div>
    </div>
    <%
        }
    %>
</div>