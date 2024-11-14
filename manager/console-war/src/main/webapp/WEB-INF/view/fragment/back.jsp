<%@ page pageEncoding="UTF-8" %>

<%
    String modelName = BackFilter.getBackModel(session, qzRequest);
    if (Utils.notBlank(modelName)) {
%>
<div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
    <div class="form-btn">
        <a modelname="<%=modelName%>"
           action-type="<%=BackFilter.BACK_URI%>" class="btn" onclick="returnHref(this);"
           href="javascript:void(0);" back-link="<%=PageUtil.buildRequestUrl(request, response, qzRequest, HtmlView.FLAG, BackFilter.BACK_URI)%>">
            <i class="icon icon-reply"></i>
            <%=I18n.getKeyI18n("page.return")%>
        </a>
    </div>
</div>
<%
    }
%>
