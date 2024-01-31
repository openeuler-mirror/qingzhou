<%@page contentType="text/html" pageEncoding="UTF-8" %>

<%
String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <base href="<%=contextPath%>/">
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="https://gitee.com/openeuler/qingzhou">
    <title>Qingzhou Console</title>
    <link type="image/x-icon" rel="shortcut icon" href="<%=contextPath%>/static/images/favicon.svg">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/lib/zui/css/zui.min.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/lib/datetimepicker/datetimepicker.min.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/css/index.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/css/nice-select.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/lib/multiple-select/multiple-select.min.css">

    <style type="text/css">
        /* list.jsp */
        table.table.table-striped.table-hover th {
            color: black;
            font-weight: unset;
        }

        /* overview.jsp */
        b, strong {
            font-weight: normal;
        }

        .panel > .panel-heading {
            color: black;
        }

        /* tree.jsp */
        #jndiNav li a {
            height: 44px;
            line-height: 30px;
            vertical-align: middle;
            text-align: left;
        }

        #jndiNav li.active a {
            color: #DF2525;
            border: 0px;
            background-color: #FFF5F5;
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
            background-color: #fff;
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
            background: #F9FAFB;
            border: 1px solid #DCDCDC;
        }

        .kv a {
            color: black;
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
    <script type="text/javascript" src="<%=contextPath%>/static/js/jquery.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/jquery.form.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/jsencrypt.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/zui/js/zui.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/datetimepicker/datetimepicker.min.js"></script>
    <%--  echarts   https://echarts.apache.org/zh/builder.html  定制的 5.3.0 如果缺少要用的模块请重新定制 --%>
    <%--  echarts  当前定制 模块有 柱状图 折线图 饼图 直角坐标系 日历 标题 图例 提示框 svg 兼容IE8 工具集 代码压缩 --%>
    <script type="text/javascript" src="<%=contextPath%>/static/js/echarts.min.js?v=5.3.0"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/msg.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/layer/layer.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/multiple-select/multiple-select.min.js"></script>
    <script src="<%=contextPath%>/static/lib/multiple-select/locale/multiple-select-locale-all.min.js"></script>
    <%@ include file="../fragment/head.jsp" %>
</head>

<body style="overflow:hidden;">
    <!--[if lt IE 8]>
    <div class="alert alert-danger"><%=PageBackendService.getMasterAppI18NString( "page.browser.outdated")%></div>
    <![endif]-->

    <%@ include file="../fragment/navbar.jsp" %>

    <main>
        <section class="tab-box">
            <ul>
                <li class="active" central="true">
                    <a href="javascript:void(0);">
                        <i class="icon icon-resize"></i>
                        <label><%=PageBackendService.getMasterAppI18NString( "page.index.centralized")%></label>
                    </a>
                </li>
            </ul>
        </section>

        <section class="content-box">
            <ul>
                <li class="active" central="true">
                    <%-- 左侧菜单 --%>
                    <aside class="main-sidebar">
                        <div class="sidebar sidebar-scroll">
                            <ul class="sidebar-menu" data-widget="tree">
                                <%
                                // 菜单
                                List<MenuItem> menuList = PageBackendService.getAppMenuList(currentUser, FrameworkContext.SYS_APP_MASTER);
                                out.print(PageBackendService.buildMenuHtmlBuilder(menuList, request, response, ViewManager.htmlView, FrameworkContext.MANAGE_TYPE_APP, FrameworkContext.SYS_APP_MASTER, qzRequest.getModelName()));
                                %>
                            </ul>
                        </div>

                        <div class="menu-toggle-btn">
                            <a href="javascript:void(0);" data-toggle="push-menu">
                                <i class="icon icon-sliders"></i>
                            </a>
                        </div>
                    </aside>

                    <section class="main-body">
                        <%-- 面包屑分级导航 --%>
                        <%@ include file="../fragment/breadcrumb.jsp" %>

                        <%-- 首页面主体部分 --%>
                        <%@ include file="../fragment/info.jsp" %>
                    </section>
                </li>
            </ul>
        </section>
    </main>
    
    <div id="mask-loading" class="mask-loading">
        <div class="loading"></div>
    </div>

    <script type="text/javascript">
        var global_setting = {
            check2FA: '<%=ConsoleConstants.LOGIN_2FA%>',
            separa: '<%=ConsoleConstants.DATA_SEPARATOR%>',
            downdloadGroupSepara: '<%=ConsoleConstants.GROUP_SEPARATOR%>',
            locale: '<%=(I18n.isZH() ? "zh-CN":"en-US")%>',
            pageLang: '<%=(I18n.isZH() ? "zh_cn":"en")%>',
            pageErrorMsg: '<%=PageBackendService.getMasterAppI18NString("page.error")%>',
            pageConfirmTitle: '<%=PageBackendService.getMasterAppI18NString("page.confirm.title")%>',
            confirmBtnText: '<%=PageBackendService.getMasterAppI18NString("page.confirm")%>',
            cancelBtnText: '<%=PageBackendService.getMasterAppI18NString("page.cancel")%>',
            notLogin: '<%=PageBackendService.getMasterAppI18NString("page.login.need")%>',
            encrypt_key_size: '<%=PageBackendService.getKeySize()%>',
            reloginBtnText: '<%=PageBackendService.getMasterAppI18NString("page.relogin")%>',
            iknowBtnText: '<%=PageBackendService.getMasterAppI18NString("page.gotit")%>',
            switchLang: '<%=PageBackendService.getMasterAppI18NString("page.lang.switch.confirm")%>',
            logout: '<%=PageBackendService.getMasterAppI18NString("page.logout.confirm")%>',
            downloadlistName: '<%=DownloadModel.ACTION_NAME_DOWNLOADLIST%>',
            downloadTip: '<%=PageBackendService.getMasterAppI18NString("page.download.log.tip")%>',
            actionName_target: '<%=FrameworkContext.SYS_ACTION_MANAGE%>',
            downloadFileNames: '<%=DownloadModel.PARAMETER_DOWNLOAD_FILE_NAMES%>',
            showAction: '<%=ShowModel.ACTION_NAME_SHOW%>',
            downloadCheckAll: '<%=PageBackendService.getMasterAppI18NString("page.download.checkall")%>',
            downloadTaskTip: '<%=PageBackendService.getMasterAppI18NString("page.download.tasktip")%>',
            layerTitle2FA: '<%=PageBackendService.getMasterAppI18NString("page.layertitle.2fa")%>',
            networkError: '<%=PageBackendService.getMasterAppI18NString("page.error.network")%>',
            placeholder2FA: '<%=PageBackendService.getMasterAppI18NString("page.placeholder.2fa")%>',
            bindSuccess2FA: '<%=PageBackendService.getMasterAppI18NString("page.bindsuccess.2fa")%>',
            bindFail2FA: '<%=PageBackendService.getMasterAppI18NString("page.bindfail.2fa")%>',
            passwordChangedMsg: '<%=PageBackendService.getMasterAppI18NString("page.password.changed")%>',
            resetPasswordUrl: '<%="/password/update"%>',
            searchHiddenTip: '<%=PageBackendService.getMasterAppI18NString("page.search.hidden")%>',
            passwordConfirmFailed: '<%=PageBackendService.getMasterAppI18NString("password.confirm.notequal")%>',
            SINGLE_FIELD_VALIDATE_PARAM: '<%=ConsoleConstants.SINGLE_FIELD_VALIDATE_PARAM%>'
        };
        var searchUrl = '<%=PageBackendService.encodeURL( response, contextPath + "/search")%>';
    </script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/main.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/index.js"></script>
    <script type="text/javascript">
        tooltip(".tooltips", {transition: true, time: 200});
    </script>
</body>
</html>
