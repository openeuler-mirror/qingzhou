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
    <meta name="author" content="www.openeuler.org">
    <title>QingZhou Console</title>
    <link type="image/x-icon" rel="shortcut icon" href="<%=contextPath%>/static/images/favicon.svg">
    <%--  echarts   https://echarts.apache.org/zh/builder.html  定制的 4.9.0 如果缺少要用的模块请重新定制 --%>
    <%--  echarts  当前定制 模块有 柱状图 折线图 饼图 直角坐标系 日历 标题 图例 提示框 svg 兼容IE8 工具集 代码压缩 --%>
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/lib/zui/css/zui.min.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/lib/datetimepicker/datetimepicker.min.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/css/index.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/css/nice-select.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/lib/multiple-select/multiple-select.min.css">
    <link type="text/css" rel="stylesheet" href="<%=contextPath%>/static/lib/intro/introjs.css">

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

        /* 用于用户引导 */
        #guide {
            position: relative;
            width: 98%;
            height: 0;
            padding-top: 98%;
        }

        #guide .guideImg {
            position: absolute;
            width: 100%;
            top: 0;
            left: 0;
        }

        #guide .guideLayer {
            position: absolute;
            border: 0px;
        }

        #guide .guideMarker {
            position: absolute;
            min-width: 3px;
            min-height: 3px;
        }
    </style>
    <%--注意：后面的<!--\>一定不能省略，否则在 IE 之外的浏览器就无法加载 jQuery --%>
    <script type="text/javascript" src="<%=contextPath%>/static/js/jquery.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/jquery.form.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/jsencrypt.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/zui/js/zui.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/datetimepicker/datetimepicker.min.js"></script>
    <%--  echarts   https://echarts.apache.org/zh/builder.html  定制的4.9.0 如果缺少要用的模块请重新定制 --%>
    <%--  echarts  当前定制  模块有 柱状图  折线图 饼图  直角坐标系 日历   标题 图例 提示框  svg 兼容IE8 工具集 代码压缩 --%>
    <script type="text/javascript" src="<%=contextPath%>/static/js/echarts.min.js?v=5.3.0"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/js/msg.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/layer/layer.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/multiple-select/multiple-select.min.js"></script>
    <script type="text/javascript"
            src="<%=contextPath%>/static/lib/multiple-select/locale/multiple-select-locale-all.min.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/lib/intro/intro.js"></script>
</head>

<%@ include file="../fragment/head.jsp" %>
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
                            List<Properties> menuList = PageBackendService.getAppMenuList(currentUser, ConsoleConstants.MASTER_APP_NAME);
                            out.print(PageBackendService.buildMenuHtmlBuilder(menuList, currentUser, request, response, ViewManager.htmlView, ConsoleConstants.MASTER_APP_NAME, qzRequest.getModelName()));
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
                    <%@ include file="../fragment/home.jsp" %>
                </section>
            </li>
        </ul>
    </section>
</main>
<div id="mask-loading" class="mask-loading">
    <div class="loading"></div>
</div>

<!-- 用户向导 -->
<div id="guide" class="guide" style="display:none;">
    <img id="guideImg" src="<%=contextPath%>/static/images/guide/index.png" class="guideImg" alt="">
    <div class="guideLayer"></div>
</div>

<script type="text/javascript">
    var global_setting = {
        check2FA: '<%=ConsoleConstants.LOGIN_2FA%>',
        separa: '<%=ConsoleConstants.DATA_SEPARATOR%>',
        downdloadGroupSepara: '<%=ConsoleConstants.GROUP_SEPARATOR%>',
        locale: '<%=(I18n.getI18nLang().isZH() ? "zh-CN":"en-US")%>',
        pageLang: '<%=(I18n.getI18nLang().isZH() ? "zh_cn":"en")%>',
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
        actionName_target: '<%=ConsoleUtil.ACTION_NAME_TARGET%>',
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
    var searchUrl = '<%=PageBackendService.encodeURL(request, response, contextPath + "/search")%>';
</script>
<script type="text/javascript" src="<%=contextPath%>/static/js/main.js"></script>
<script type="text/javascript" src="<%=contextPath%>/static/js/index.js"></script>
<script type="text/javascript">
    tooltip(".tooltips", {transition: true, time: 200});
    // Beginner Guide
    var guideInited = false;
    var defaultBgImageSrc = $("#guide>#guideImg").attr("src");
    $("#guide-btn").bind("click", function () {
        $(document.body).children("header,main").hide();
        $("#guide").show();
        var guideOptions = {
            "prevLabel": "&larr; <%=PageBackendService.getMasterAppI18NString("page.guide.previous")%>",
            "nextLabel": "<%=PageBackendService.getMasterAppI18NString("page.guide.next")%> &rarr;",
            "skipLabel": "<%=PageBackendService.getMasterAppI18NString("page.guide.skip")%>",
            "doneLabel": "<%=PageBackendService.getMasterAppI18NString("page.guide.finish")%>",
            "exitOnOverlayClick": false, "overlayOpacity": 0.5, "showStepNumbers": false
        };
        guideOptions["steps"] = [
            {"element": "#JiyYEh", "intro": '<%=PageBackendService.getMasterAppI18NString( "page.guide.pwd")%>', "image": "index.png", "position": "left", "rl": 92.98618490967057, "rt": 0.21253985122210414, "rw": 3.9319872476089266, "rh": 3.4006376195536663},
            {"element": "#NcDLKd", "intro": '<%=PageBackendService.getMasterAppI18NString( "page.guide.help")%>', "image": "index.png", "position": "bottom", "rl": 84.9096705632306, "rt": 0.26567481402763016, "rw": 4.0913921360255046, "rh": 3.1880977683315623},
            {"element": "#SRvTcb", "intro": '<%=PageBackendService.getMasterAppI18NString( "page.guide.home")%>', "image": "index.png", "position": "right", "rl": -0.3188097768331562, "rt": 7.1200850159404885, "rw": 15.834218916046758, "rh": 3.5600425079702442},
            {"element": "#tPFOOB", "intro": '<%=PageBackendService.getMasterAppI18NString( "page.guide.res")%>', "image": "create-ds.png", "position": "top", "rl": -0.21253985122210414, "rt": 23.645058448459086, "rw": 15.037194473963869, "rh": 3.1349628055260363}
        ];
        if (!guideInited) {
            guideInited = true;
            for (var i = 0; i < guideOptions["steps"].length; i++) {
                $("#guide").append("<div id='" + guideOptions["steps"][i]["element"].substring(1) + "' class='guideMarker'></div>");
            }
            var adjustGuidePosition = function () {
                var guideContainer = document.getElementById("guide");
                for (var i = 0; i < guideOptions["steps"].length; i++) {
                    $(guideOptions["steps"][i]["element"]).css({
                        "top": guideOptions["steps"][i]["rt"] * guideContainer.clientHeight / 100 + "px",
                        "left": guideOptions["steps"][i]["rl"] * guideContainer.clientWidth / 100 + "px",
                        "width": guideOptions["steps"][i]["rw"] * guideContainer.clientWidth / 100 + "px",
                        "height": guideOptions["steps"][i]["rh"] * guideContainer.clientHeight / 100 + "px"
                    });
                }
            };
            adjustGuidePosition();
            // 窗口大小改变事件
            $(window).resize(adjustGuidePosition);
        }
        // 上一页 / 下一页 是否切换背景图片
        guideOptions.stepAction = function (image) {
            if (image === undefined || image === null || image === "") {
                image = defaultBgImageSrc.substring(defaultBgImageSrc.lastIndexOf("/") + 1);
            }
            var imgSrc = $("#guide>#guideImg").attr("src");
            if (!imgSrc.endsWith(image)) {
                $("#guide>#guideImg").attr("src", imgSrc.substring(0, imgSrc.lastIndexOf("/") + 1) + image);
            }
        };
        // 引导关闭时
        guideOptions.onClose = function () {
            $("#guide>#guideImg").attr("src", defaultBgImageSrc);
            $(document.body).children("header,main").show();
            $("#guide").hide();
            window.scrollBy(0, -500);
        };
        introJs().setOptions(guideOptions).start();
    });
</script>
</body>
</html>
