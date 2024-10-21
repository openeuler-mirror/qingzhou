<%@ page pageEncoding="UTF-8" %>

<%-- 面包屑分级导航 --%>
<ol class="breadcrumb" style="margin-left: 5px; font-size: 15px; margin-bottom: 0; min-width: 300px; padding: 10px 5px !important;">
    <li class="active" style="margin-left:-5px;">
        <div class="model-info">
            <span><%=I18n.getModelI18n(qzApp, "model." + qzModel)%></span>
            <span class="tooltips" data-tip='<%=I18n.getModelI18n(qzApp, "model.info." + qzModel)%>'
                  data-tip-arrow="bottom-right" style="line-height:25px;">
                <i class="icon icon-question-sign"></i>
            </span>
        </div>
    </li>
    <%
        String actionDesc = I18n.getModelI18n(qzApp, "model.action." + qzModel + "." + qzAction);
        if (!Objects.equals(actionDesc, I18n.getModelI18n(qzApp, "model." + qzModel))) {
    %>
    <li class="active"><%=actionDesc%></li>
    <%
        }
    %>
</ol>

<hr style="margin-top: 4px;">