<%@ page pageEncoding="UTF-8" %>

<%
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="zh">
<head>
    <base href="<%=contextPath%>/">
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="renderer" content="webkit|ie-comp|ie-stand">
    <meta name="viewport"
          content="width=device-width,initial-scale=1.0,minimum-scale=0.5,shrink-to-fit=no,user-scalable=yes">
    <meta name="author" content="https://gitee.com/openeuler/qingzhou">
    <title>Qingzhou Console</title>
    <link type="image/x-icon" rel="shortcut icon" href="<%=contextPath%>/static/images/favicon.svg">
    <link rel="stylesheet" href="<%=contextPath%>/static/lib/zui/css/zui.min.css">
    <link rel="stylesheet" href="<%=contextPath%>/static/lib/datetimepicker/datetimepicker.min.css">
    <link rel="stylesheet" href="<%=contextPath%>/static/css/index.css">
    <link rel="stylesheet" href="<%=contextPath%>/static/css/nice-select.css">
    <link rel="stylesheet" href="<%=contextPath%>/static/lib/multiple-select/multiple-select.min.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/lib/daterangepicker/daterangepicker.css">


    <style>
        /* list.jsp */
        table.table.table-striped.table-hover th {
            color: black;
            font-weight: unset;
        }

        .panel > .panel-heading {
            color: black;
        }

        /* sortable.jsp */
        .form-control-sortable {
            display: block;
            width: calc(100% + 5px);
            margin-left: -2px;
            height: auto;
            padding: 5px 0px;
            color: #222;
            font-size: 13px;
            line-height: 1.53846154;
            background-color: #fff;
            -webkit-transition: border-color ease-in-out .15s, -webkit-box-shadow ease-in-out .15s;
            -o-transition: border-color ease-in-out .15s, box-shadow ease-in-out .15s;
            transition: border-color ease-in-out .15s, -webkit-box-shadow ease-in-out .15s;
            transition: border-color ease-in-out .15s, box-shadow ease-in-out .15s;
            transition: border-color ease-in-out .15s, box-shadow ease-in-out .15s, -webkit-box-shadow ease-in-out .15s;
        }

        ul.sortable, ul.sortable-add {
            list-style: none;
            padding: 0px 2px;
        }

        .sortable table {
            background: #FFFFFF !important;
            border: 1px solid #DCDCDC !important;
        }

        .sortable table tbody tr {
            padding-bottom: 5px;
            height: 32px !important;
        }

        .sortable table tr td {
            padding: 5px !important;
        }

        .sortable td.editable {
            padding: 0px 0px !important;
            vertical-align: middle;
            height: 32px;
            line-height: 32px;
        }

        .sortable td.narrow {
            width: 32px !important;
            text-align: center;
            background: #F9FAFB;
            border: 1px solid #DCDCDC;
        }

        .sortable a {
            color: black;
            text-decoration: none;
        }

        .sortable .draggable a {
            cursor: move;
        }

        .sortable .editable label {
            margin: 0px 0px !important;
            padding-left: 3px;
        }

        .sortable .editable input {
            width: 100%;
            border: 0px;
            padding: 0px 0px;
            padding-left: 3px;
            margin: 0px;
            border-radius: 0;
            height: 30px;
            line-height: 30px;
        }

        .sortable .editable input:focus {
            border: 0px !important;
            box-shadow: none !important;
        }

        .sortable .read-only {
            cursor: not-allowed;
        }

        .sortable-add {
            border: 0px;
        }

        .sortable-add li {
            height: 32px;
            border: 1px dashed #DCDCDC;
            line-height: 32px;
            text-align: center;
            vertical-align: middle;
            padding-left: 3px;
            padding-right: 5px;
        }

        .sortable-add a {
            color: black;
            text-decoration: none;
        }

        /* kv.jsp */
        .form-control-kv {
            display: block;
            width: calc(100% + 5px);
            margin-left: -2px;
            height: auto;
            padding: 5px 0px;
            color: #222;
            font-size: 13px;
            line-height: 1.53846154;
            /*background-color: #fff;*/
            -webkit-transition: border-color ease-in-out .15s, -webkit-box-shadow ease-in-out .15s;
            -o-transition: border-color ease-in-out .15s, box-shadow ease-in-out .15s;
            transition: border-color ease-in-out .15s, -webkit-box-shadow ease-in-out .15s;
            transition: border-color ease-in-out .15s, box-shadow ease-in-out .15s;
            transition: border-color ease-in-out .15s, box-shadow ease-in-out .15s, -webkit-box-shadow ease-in-out .15s;
        }

        table.kv {
            background: #FFFFFF !important;
            border: 1px solid #DCDCDC !important;
        }

        .kv tbody tr {
            padding-bottom: 5px;
            height: 32px !important;
        }

        .kv tr td {
            padding: 5px !important;
            background-color: var(--bg-color-kv-tr-td, #ffffff);
        }

        .kv td.edit-kv {
            padding: 0px 0px !important;
            vertical-align: middle;
            height: 32px;
            line-height: 32px;
        }

        .kv .narrow {
            width: 32px !important;
            text-align: center;
            background: var(--bg-color-kv-narrow, #F9FAFB);
            border: 1px solid #DCDCDC;
        }

        .kv a {
            color: var(--color-kv-a, black);
            text-decoration: none;
        }

        .kv .edit-kv input {
            width: 100%;
            border: 0px;
            padding: 0px 0px;
            padding-left: 3px;
            margin: 0px;
            border-radius: 0;
            height: 30px;
            line-height: 30px;
        }
    </style>
    <%--注意：后面的<!--\>一定不能省略，否则在 IE 之外的浏览器就无法加载 jQuery --%>
    <script src="<%=contextPath%>/static/js/jquery.min.js"></script>
    <script src="<%=contextPath%>/static/js/jquery.form.min.js"></script>
    <script src="<%=contextPath%>/static/js/jsencrypt.min.js"></script>
    <script src="<%=contextPath%>/static/lib/zui/js/zui.min.js"></script>
    <script src="<%=contextPath%>/static/lib/datetimepicker/datetimepicker.min.js"></script>
    <%--  echarts   https://echarts.apache.org/zh/builder.html  定制的 5.3.0 如果缺少要用的模块请重新定制 --%>
    <%--  echarts  当前定制 模块有 柱状图 折线图 饼图 直角坐标系 日历 标题 图例 提示框 svg 兼容IE8 工具集 代码压缩 --%>
    <script src="<%=contextPath%>/static/js/echarts.min.js?v=5.3.0"></script>
    <script src="<%=contextPath%>/static/js/msg.js"></script>
    <script src="<%=contextPath%>/static/lib/layer/layer.js"></script>
    <script src="<%=contextPath%>/static/lib/multiple-select/multiple-select.min.js"></script>
    <script src="<%=contextPath%>/static/lib/multiple-select/locale/multiple-select-locale-all.min.js"></script>
    <script src="<%=contextPath%>/static/lib/marked/marked.min.js"></script>
    <script src="<%=contextPath%>/static/lib/muuri/muuri.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/daterangepicker/moment.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/daterangepicker/daterangepicker.min.js"></script>
</head>

<%@ include file="../fragment/head.jsp" %>

<body class="<%=(themeMode == null || themeMode.isEmpty()) ? "" : (themeMode + "-mode")%>" style="overflow:hidden;">
<!--[if lt IE 8]>
    <div class="alert alert-danger"><%=I18n.getKeyI18n( "page.browser.outdated")%></div>
    <![endif]-->

<%@ include file="../fragment/navbar.jsp" %>

<main>
    <section class="tab-box">
        <ul preTab="defaultTab">
            <li id="defaultTab" fixed="true">
                <a href="javascript:void(0);">
                    <i class="icon icon-resize"></i>
                    <label><%=I18n.getKeyI18n("page.index.centralized")%>
                    </label>
                </a>
            </li>
        </ul>
    </section>

    <section class="content-box">
        <ul>
            <li id="defaultTabBox" class="active" fixed="true">
                <%-- 左侧菜单 --%>
                <%@ include file="../fragment/menu.jsp" %>
            </li>
        </ul>
    </section>
</main>

<div id="mask-loading" class="mask-loading">
    <div class="loading"></div>
</div>

<script>
    var global_setting = {
        "DOWNLOAD_FILE_NAMES_SP": '<%=DownloadData.DOWNLOAD_FILE_NAMES_SP%>',
        "downdloadGroupSepara": '<%=DownloadData.DOWNLOAD_FILE_GROUP_SP%>',
        "downloadFileNames": '<%=DownloadData.DOWNLOAD_FILE_NAMES%>',
        "checkOtp": '<%=LoginManager.LOGIN_OTP%>',
        "locale": '<%=(I18n.isZH() ? "zh-CN" : "en-US")%>',
        "pageLang": '<%=(I18n.isZH() ? "zh_cn" : "en")%>',
        "langFlag": '<%=I18n.getI18nLang().flag%>',
        "pageErrorMsg": '<%=I18n.getKeyI18n("page.error")%>',
        "pageConfirmTitle": '<%=I18n.getKeyI18n("page.confirm.title")%>',
        "confirmBtnText": '<%=I18n.getKeyI18n("page.confirm")%>',
        "cancelBtnText": '<%=I18n.getKeyI18n("page.return")%>',
        "notLogin": '<%=I18n.getKeyI18n("page.login.need")%>',
        "encrypt_key_size": '<%=SystemController.getKeySize()%>',
        "reloginBtnText": '<%=I18n.getKeyI18n("page.relogin")%>',
        "iknowBtnText": '<%=I18n.getKeyI18n("page.gotit")%>',
        "switchLang": '<%=I18n.getKeyI18n("page.lang.switch.confirm")%>',
        "switchText": '<%=I18n.getKeyI18n("page.list.switch.confirm")%>',
        "logout": '<%=I18n.getKeyI18n("page.logout.confirm")%>',
        "downloadTip": '<%=I18n.getKeyI18n("page.download.log.tip")%>',
        "downloadCheckAll": '<%=I18n.getKeyI18n("page.download.checkall")%>',
        "downloadTaskTip": '<%=I18n.getKeyI18n("page.download.tasktip")%>',
        "layerTitleOtp": '<%=I18n.getKeyI18n("page.layertitle.otp")%>',
        "networkError": '<%=I18n.getKeyI18n("page.error.network")%>',
        "placeholderOtp": '<%=I18n.getKeyI18n("page.placeholder.otp")%>',
        "bindSuccessOtp": '<%=I18n.getKeyI18n("page.bindsuccess.otp")%>',
        "bindFailOtp": '<%=I18n.getKeyI18n("page.bindfail.otp")%>',
        "searchHiddenTip": '<%=I18n.getKeyI18n("page.search.hidden")%>',
        "downloadView": '<%=DownloadView.FLAG%>',
        "htmlView": '<%=HtmlView.FLAG%>',
        "jsonView": '<%=JsonView.FLAG%>',
        "showActionName": '<%=Show.ACTION_SHOW%>',
        "actionId_app_manage": '<%=DeployerConstants.APP_SYSTEM + "-" + DeployerConstants.MODEL_APP + "-" + DeployerConstants.ACTION_MANAGE%>',
        "actionId_app_stop-delete": '<%=DeployerConstants.APP_SYSTEM + "-" + DeployerConstants.MODEL_APP + "-" + Delete.ACTION_DELETE%>' + ',' + '<%=DeployerConstants.APP_SYSTEM + "-" + DeployerConstants.MODEL_APP + "-" + DeployerConstants.ACTION_STOP%>',
        "data": '<%=DashboardDataBuilder.DASHBOARD_FIELD_DATA%>',
        "unit": '<%=DashboardDataBuilder.DASHBOARD_FIELD_UNIT%>',
        "fields": '<%=DashboardDataBuilder.DASHBOARD_FIELD_FIELDS%>',
        "info": '<%=DashboardDataBuilder.DASHBOARD_FIELD_INFO%>',
        "title": '<%=DashboardDataBuilder.DASHBOARD_FIELD_TITLE%>',
        "max": '<%=DashboardDataBuilder.DASHBOARD_FIELD_MAX%>',
        "used": '<%=DashboardDataBuilder.DASHBOARD_FIELD_USED%>',
        "xAxis": '<%=DashboardDataBuilder.DASHBOARD_FIELD_XAXIS%>',
        "yAxis": '<%=DashboardDataBuilder.DASHBOARD_FIELD_YAXIS%>',
        "xAxisName": '<%=DashboardDataBuilder.DASHBOARD_FIELD_XAXIS_NAME%>',
        "yAxisName": '<%=DashboardDataBuilder.DASHBOARD_FIELD_YAXIS_NAME%>',
        "showValue": '<%=DashboardDataBuilder.DASHBOARD_FIELD_SHOWVALUE%>',
        "matrixData": '<%=DashboardDataBuilder.DASHBOARD_FIELD_MATRIXDATA%>',
        "basicData": '<%=DashboardDataBuilder.DASHBOARD_FIELD_BASIC_DATA%>',
        "gaugeData": '<%=DashboardDataBuilder.DASHBOARD_FIELD_GAUGE_DATA%>',
        "histogramData": '<%=DashboardDataBuilder.DASHBOARD_FIELD_HISTOGRAM_DATA%>',
        "shareDatasetData": '<%=DashboardDataBuilder.DASHBOARD_FIELD_SHARE_DATASET_DATA%>',
        "paramNameReturnsId": "<%=DeployerConstants.RETURNS_LINK_PARAM_NAME_RETURNSID%>",
        "actionTypeReturns": "<%=ActionType.returns_link%>",
        "matrixHeatmapData": '<%=DashboardDataBuilder.DASHBOARD_FIELD_HEATMAP_DATA%>',
        "lineChartData": '<%=DashboardDataBuilder.DASHBOARD_FIELD_LINE_CHART_DATA%>'
    };

    <%-- 显示第一个应用的管理页面--%>
    <%
	String appName = PageUtil.getAppToShow();
	if (appName != null) {
            //获取系统应用菜单图标
            String icon = SystemController.getModelInfo(DeployerConstants.APP_SYSTEM, DeployerConstants.MODEL_APP).getIcon();
    %>
    $(document).ready(function () {
        var firstAppId = "<%= DeployerConstants.MODEL_APP %>|<%= appName %>";
        var firstAppName = "<%= appName %>";
        var AppIcon = "<%= icon %>";

        var firstAppElement = {
            'data-id': firstAppId,
            'data-name': firstAppName,
            'model-icon': AppIcon,
            attr: function (name) {
                return this[name];
            }
        };

        var url = "<%=PageUtil.buildCustomUrl(request, response, qzRequest, HtmlView.FLAG, DeployerConstants.MODEL_APP, DeployerConstants.ACTION_MANAGE + "/" + appName)%>"
        initializeManager(firstAppElement, url);
    });
    <%
        }
    %>

</script>
<script src="<%=contextPath%>/static/js/dashboard.js"></script>
<script src="<%=contextPath%>/static/js/qz.js"></script>
<script src="<%=contextPath%>/static/js/main.js"></script>
<script src="<%=contextPath%>/static/js/index.js"></script>
<script>
    qz.tooltip(".tooltips", {transition: true, time: 200});
</script>
</body>
</html>
