<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    String encodedId = qzRequest.getId();
    if (ConsoleSDK.needEncode(encodedId)) {
        encodedId = ConsoleSDK.encodeId(encodedId);
    }
    String url = PageBackendService.buildRequestUrl(request, response, qzRequest, ViewManager.jsonView, MonitorModel.ACTION_NAME_MONITOR + "/" + encodedId);
%>

<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <div class="monitorPage" chartMonitor="true" data-url="<%=url%>">
        <%
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ConsoleConstants.DATE_FORMAT);
            String startTime = LocalDateTime.now().minusDays(1).format(formatter);
            String endTime = LocalDateTime.now().format(formatter);
        %>

        <div class="block-bg btn-group" style="display: flex; justify-content: right; padding-right: 0">
            <div style="padding-right: 10px">
                <input id="monitor-startTime" type="text" class="form-datetime"
                       style="padding: 5px 12px; border: 1px solid #DCDCDC"
                       value="<%=startTime%>"
                       autocomplete="off"
                       data-date-format="yyyy-mm-dd hh:ii:ss"
                       data-minute-step="3"
                       data-date-language="<%=I18n.getI18nLang()==Lang.en?"en":"zh-cn"%>"
                       placeholder="<%=PageBackendService.getMasterAppI18NString( "page.monitor.info.start-time")%>">
                -
                <input id="monitor-endTime" type="text" class="form-datetime"
                       style="padding: 5px 12px; border: 1px solid #DCDCDC"
                       value="<%=endTime%>"
                       autocomplete="off"
                       data-date-format="yyyy-mm-dd hh:ii:ss"
                       data-minute-step="3"
                       data-date-language="<%=I18n.getI18nLang()==Lang.en?"en":"zh-cn"%>"
                       placeholder="<%=PageBackendService.getMasterAppI18NString( "page.monitor.info.end-time")%>">
            </div>
            <button class="btn"
                    id="monitor-customize"><%=PageBackendService.getMasterAppI18NString( "page.monitor.info.customize")%>
            </button>
            <button class="btn"
                    id="monitor-2-hour"><%=PageBackendService.getMasterAppI18NString( "page.monitor.info.last-2h")%>
            </button>
            <button class="btn"
                    id="monitor-30-min"><%=PageBackendService.getMasterAppI18NString( "page.monitor.info.last-30min")%>
            </button>
            <button class="btn active"
                    id="monitor-real-time"><%=PageBackendService.getMasterAppI18NString( "page.monitor.info.real-time")%>
            </button>
        </div>

        <div class="panel" style="border-radius: 2px; border-color:#EFEEEE; background-color: #FFFFFF;">
            <div class="panel-body" style="word-break: break-all">
                <div class="block-bg" container="chart" style="height: 600px;width: 100%"></div>
            </div>
        </div>
    </div>

    <div class="block-bg" style="margin-top: 15px; height: 64px; text-align: center;">
        <div class="form-btn">
            <a href="javascript:void(0);" onclick="tw.goback(this);" btn-type="goback" class="btn">
                <%=PageBackendService.getMasterAppI18NString( "page.cancel")%>
            </a>
        </div>
    </div>
</div>
