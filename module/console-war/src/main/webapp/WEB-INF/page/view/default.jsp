<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    if (qzRequest == null || qzResponse == null || modelManager == null) {
        return; // for 静态源码漏洞扫描
    }
    final boolean hasId = ConsoleWarHelper.hasIDField(qzRequest);
    if (!qzResponse.getDataList().isEmpty()) {
%>

<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <div class="block-bg">
        <table class="table table-hover">
            <tr>
                <td>
                    <%=PageBackendService.getMasterAppI18NString( "page.status")%>:
                </td>
                <td>
                    <%=qzResponse.isSuccess()%>
                </td>
            </tr>
            <tr>
                <td>
                    <%=PageBackendService.getMasterAppI18NString( "page.msg")%>:
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
                                        <a target="_blank" href="<%=key%>">
                                            <%=key%>
                                        </a>
                                        <%
                                            } else {
                                                out.print(key);
                                            }
                                        %>
                                </span>
                        <span class="value">
                                    <%
                                        if (StringUtil.notBlank(value)) {
                                            if (value.startsWith("http")) {
                                    %>
                                            <a target="_blank" href="<%=value%>">
                                                <%=value%>
                                            </a>
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

    <%
    if (modelManager.getModelAction(qzRequest.getModelName(), ListModel.ACTION_NAME_LIST) != null) {
        %>
        <div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
            <div class="form-btn">
                <a href="javascript:void(0);" btn-type="goback"
                   onclick="tw.goback(this);" class="btn">
                    <!--<i class="icon icon-undo"></i>-->
                    <%=PageBackendService.getMasterAppI18NString( "page.cancel")%>
                </a>
            </div>
        </div>
        <%
    }
    %>
</div>
<%}%>