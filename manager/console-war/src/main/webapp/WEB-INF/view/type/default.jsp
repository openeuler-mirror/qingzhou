<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    if (qzResponse.getDataList().isEmpty()) return;
%>

<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <div class="block-bg">
        <table class="table table-hover">
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
    </div>
</div>