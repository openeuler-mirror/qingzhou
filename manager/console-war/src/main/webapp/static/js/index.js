var tabMap = {};
/**
 * 获取全局配置项
 * @param key 配置 key
 */
function getSetting(key) {
    var settings = eval("(typeof global_setting !== 'undefined') ? global_setting : {}");
    return settings[key] ? settings[key] : key;
};

// 当前页面主活动区域
function getActiveTabContent() {
    return $(".content-box>ul>li")[$(".tab-box>ul>li.active").index()];
};

/**
 * 获取限定区域
 * @param {type} inSubTab (false 返回当前 Tab 标签 | true 返回二级Tab标签中的活动Tab标签作为限定区域)
 */
function getRestrictedArea(inSubTab) {
    var restrictedArea = getActiveTabContent();
    if (!inSubTab) {
        return restrictedArea;
    }
    var tabContainer = $("div.tab-container:first", restrictedArea);
    if ($(tabContainer).length > 0) {
        var qzTab = tabMap[$(tabContainer).attr("id")];
        return qzTab.getActiveTab();
    }
    return restrictedArea;
};

$(document).ready(function () {
    // ITAIT-4984 微软自研浏览器 Edge 样式特殊处理，解决滚动条样式问题
    var browserInfo = qz.browserNV();
    if (browserInfo.core === "Edge" && browserInfo.v <= 60.0) {
        $(".main-body").css({"min-height": "calc(-100px + 100%)", "height": "auto", "top": "100px", "bottom": "100px"});
    }

    // 响应式小屏模式下，点击完菜单，自动隐藏左侧菜单栏
    $(".sidebar li a").click(function () {// TODO 需要考虑多Tab标签的情况
        if ($(document.body).hasClass("sidebar-open") && $(this).attr("href").indexOf("/") > -1) {
            $(document.body).toggleClass("sidebar-open");
        }
    });
    $(document.body).click(function (e) {
        if ($(document.body).hasClass("sidebar-open") && !$(e.target).hasClass(".sidebar-toggle") && $(e.target).parents(".sidebar-toggle").length === 0
            && !$(e.target).hasClass(".main-sidebar") && $(e.target).parents(".main-sidebar").length === 0) {
            $(document.body).toggleClass("sidebar-open");
        }
    });

    //切换主题模式点击事件
    $("#switch-mode-btn").unbind("click").bind("click", function () {
        var icon = $("i", this);
        var $this = this;
        var themeUrl = $(this).attr("themeUrl");
        var nowTheme = $(this).attr("theme");
        var toTheme = $(this).attr("theme") === "" ? "dark" : "";
        if (themeUrl.indexOf("?") > 0) {
            themeUrl = themeUrl.substr(0, themeUrl.lastIndexOf("/") + 1) + toTheme + themeUrl.substring(themeUrl.indexOf("?"));
        } else {
            themeUrl = themeUrl.substr(0, themeUrl.lastIndexOf("/") + 1) + toTheme;
        }
        $.post(themeUrl, {}, function (themeTxt) {
            $("body").removeClass(nowTheme + "-mode");
            if (themeTxt !== "") {
                $("body").addClass(themeTxt + "-mode");
            }
            if (icon.hasClass("icon-moon")) {
                icon.removeClass("icon-moon").addClass("icon-sun");
            } else {
                icon.removeClass("icon-sun").addClass("icon-moon");
            }
            $($this).attr("theme", themeTxt);
        }, "text");
    });
    // 切换语言点击事件
    $("#switch-lang>ul>li>a").unbind("click").bind("click", function (e) {
        e.preventDefault();
        var url = $(this).attr("href");
        showConfirm(getSetting("switchLang"), {
            "title": getSetting("pageConfirmTitle"),
            "btn": [getSetting("confirmBtnText"), getSetting("cancelBtnText")]
        }, function () {
            $.post(url, {}, function (html) {
                window.location.reload();
            }, "html");
        }, function () {
        });
        return false;
    });
    // 用户退出点击事件
    $("#logout-btn").unbind("click").bind("click", function (e) {
        e.preventDefault();
        var url = $(this).attr("href");
        showConfirm(getSetting("logout"), {
            "btn": [getSetting("confirmBtnText"), getSetting("cancelBtnText")]
        }, function () {
            window.location.href = url;
        }, function () {
        });
        return false;
    });
    // 用户修改密码点击事件
    $("#reset-password-btn").unbind("click").bind("click", function (e) {
        e.preventDefault();
        $(".tab-box>ul>li[fixed='true']").click();
        var restrictedArea = getRestrictedArea();
        var lis = $(".sidebar-menu .active", restrictedArea);// TODO 需要考虑二级 Tab 标签的情况
        lis.removeClass("menu-open active");
        for (var i = 0; i < lis.length; i++) {
            var uls = lis[i].querySelectorAll('ul');
            for (var j = 0; j < uls.length; j++) {
                uls[j].style.display = 'none';
            }
        }
        $(".sidebar-menu .expandsub", restrictedArea).removeClass("menu-open expandsub");
        var matchPart = $(this).attr("href");
        var menuItemLink = $("ul.sidebar-menu li a[href*='" + matchPart + "']", restrictedArea);
        if (menuItemLink.length > 0) {
            $(menuItemLink).parents("li.treeview").addClass("menu-open active");
            $(menuItemLink).parents("ul.treeview-menu").show();
            $(menuItemLink).parent().addClass("active");
            $(menuItemLink).parents(".sidebar.sidebar-scroll").animate({scrollTop: $(menuItemLink).parents(".sidebar.sidebar-scroll").scrollTop() + $(menuItemLink).parent().offset().top - $(menuItemLink).parents(".sidebar.sidebar-scroll").offset().top}, 1000);
        }
        qz.fill(matchPart, {}, $(".main-body").first(), false, null);
        return false;
    });

    // 设置或重新设置（如事件绑定、赋初始值等）
    setOrReset();
    $(document.body).resize(function() {
        $("div.tab-container").css({"height": ($(document.body).height() - 170) + "px"});
    });
});

// 绑定本机、集群、实例 Tab 标签事件
function bindTabEvent() {
    $(".tab-box>ul>li[loaded!='true']").attr("loaded", "true").each(function (i, dom) {
        var $this = this;
        $(this).unbind("click").bind("click", function (e) {
            e.preventDefault();
            if ($($this).hasClass("active") || (e.target.tagName === "LABEL" && $(e.target).hasClass("close"))) {
                return false;
            }
            var activeTab = $(".tab-box li.active");
            $(this).parent().attr("preTab", $(activeTab).attr("id"));
            $(activeTab).removeClass("active");
            $($(".content-box>ul>li")[$(activeTab).index()]).removeClass("active").addClass("inactive");
            $(this).addClass("active");
            $($(".content-box>ul>li")[$(this).index()]).removeClass("inactive").addClass("active");
            autoAdaptTip();
            return false;
        });
        $(".close", this).unbind("click").bind("click", function (e) {
            e.preventDefault();
            if ($($this).hasClass("active")) {
                var preTab = $("#" + $($this).parent().attr("preTab"));
                if (preTab == undefined || preTab.attr("id") == $($this).attr("id")) {
                    preTab = $("#defaultTab");
                }
                $(preTab).addClass("active");
                $($(".content-box>ul>li")[$(preTab).index()]).removeClass("inactive").addClass("active");
            } else {
                $($this).parent().attr("preTab", $(".tab-box li.active").attr("id"));
            }
            $("div.bodyDiv").each(function () {
                DashboardManager.close($(this));
            });
            $($(".content-box>ul>li")[$($this).index()]).remove();
            $($this).remove();
            autoAdaptTip();
            return false;
        });
    });
};

/**
 * 设置(如绑定事件或设置初始值等) 或 重新设置
 */
function setOrReset(container) {
    $("section.main-body", document.body).each(function () {
        $(this).children(".bodyDiv").attr("bindingId", $(this).attr("bindingId"));
    });
    // 菜单区域禁止鼠标右键
    $(".main-sidebar").unbind("contextmenu").bind("contextmenu", function (e) {
        e.preventDefault();
        return false;
    });
    // 菜单展示优化
    $("aside.main-sidebar .sidebar-menu>li[loaded!='true']").attr("loaded", "true").hover(function () {
        if ($(document.body).hasClass("sidebar-collapse")) {// TODO Need to consider the case of multiple Tab tabs.
            $(".main-sidebar .sidebar").removeClass("sidebar-scroll");
            if ($(".treeview-menu", this).length < 1) {
                $(this).addClass("optimize-sub");
            } else {
                if ($(this).offset().top < $(".main-header .navbar").height()) {
                    $(".treeview-menu", this).addClass("locate-t");
                } else {
                    var compare = $(window).height() - ($(this).offset().top + $(".treeview-menu", this).height());
                    if (compare < 30) {
                        $(".treeview-menu", this).addClass("locate-b");
                        $(this).addClass("for-locate");
                    }
                }
            }
        }
    }, function () {
        $(this).removeClass("optimize-sub for-locate");
        $(".treeview-menu", this).removeClass("locate-t locate-b");
        $(".main-sidebar .sidebar").addClass("sidebar-scroll");
    });
    // 布尔开关
    $(".switch-btn:not(.disallowed)[loaded!='true']").attr("loaded", "true").unbind("click").bind("click", function () {
        $(".switchedge", this).toggleClass("switch-bg");
        $(".circle", this).toggleClass("switch-right");
        $("input", this).val($("input", this).val() === "true" ? false : true);
        $("input", this).change();
    });
    // 多选下拉
    $("select[multiple='multiple'][loaded!='true']").attr("loaded", "true").each(function () {
        var $this = $(this);
        $(this).multipleSelect({
            locale: getSetting("locale"),
            multiple: true,
            multipleWidth: 180,
            filter: true,
            filterGroup: true,
            showClear: true,
            animate: "fade",
            onAfterCreate: function () {
                $("ul li.multiple", $($this).next(".ms-parent")).each(function () {
                    $(this).attr("title", $("label>span", this).first().text());
                    var name = $("label>input", this).first().val();
                    var color = $(this).parent().parent().parent().siblings("select[multiple='multiple']").find("option[value=" + name + "]").css("color")
                    $("label>span", this).css("color", color);
                    $(this).hover(function () {
                        $("label>span", this).css({"white-space": "normal"});
                    }, function () {
                        $("label>span", this).css({"white-space": "nowrap"});
                    });
                });
                return true;
            }
        });
    });

    // 一级 Tab 标签切换事件绑定
    bindTabEvent();
    // 下拉列表 / 可输入下拉列表
    niceSelect();
    // sortable.jsp 拖拽排序
    dragable();
    // sortablecheckbox.jsp 拖拽排序
    checkboxSortable();
    // 列表页面事件操作
    bindEventForListPage();
    // form 表单页面事件操作
    bindEventForFormPage();
    // monitor.jsp 页面加载
    initMonitorPage();
    // chart.jsp 页面加载
    initChartPage();
    // dashboard.jsp 页面加载
    DashboardManager.initialize(container ? $(".bodyDiv>div.dashboardPage", container) : undefined);
    // grid.jsp 页面初始化
    if (document.querySelectorAll(".apps").length > 0) {
        var apps = document.querySelectorAll(".apps");
        for (var i = 0; i < apps.length; i++) {
            new Muuri(apps[i], {
                items: apps[i].querySelectorAll("div.app"),
                layoutDuration: 300,
                layoutEasing: "ease",
                dragEnabled: false
            });
        }
    }
    // markdown 内容展示
    $(".markedviewText[loaded!='true']").attr("loaded", "true").each(function () {
        $(this).prev(".markedview").html(marked.parse($(this).val()));
    });
    $("label[password_label_right]").unbind("click").click("click", function () {
        if ($("i", this).hasClass("icon-eye-open")) {
            $("i", this).removeClass("icon-eye-open").addClass("icon-eye-close");
            $(this).prev().attr("type", "password");
        } else {
            $("i", this).removeClass("icon-eye-close").addClass("icon-eye-open");
            $(this).prev().attr("type", "text");
        }
    });

    // 左侧菜单点击菜单事件
    $("aside.main-sidebar[loaded!='true']", document.body).each(function () {
        $(this).attr("loaded", "true");
        var bindingId = $(this).attr("bindingId");
        qz.bindFill("aside.main-sidebar[bindingId='" + bindingId + "'] ul.sidebar-menu a[loaded!='true']", ".main-body[bindingId='" + bindingId + "']", false, true, $(this).parent(), null);
    });
};

function gotoTarget(model, action, group, field) {
    var boxes = new Array(".content-box>ul>li.active:first", ".content-box>ul>li:eq(1)", ".content-box>ul>li:eq(0)");
    var restrictedArea = getRestrictedArea();// TODO 待调整
    for (var i = 0; i < boxes.length; i++) {
        var menuItemLink = $("ul.sidebar-menu li a[modelName='" + model + "']", $(boxes[i])).not(":has(div.tab-container)");
        if (menuItemLink.length > 0) {
            if ($(menuItemLink).parent().hasClass("treeview")) {
                $(menuItemLink).parents("li.treeview").addClass("active");
            } else {
                $(menuItemLink).parents("li.treeview").addClass("menu-open active");
            }
            $(menuItemLink).parents("ul.treeview-menu").show();
            $(menuItemLink).parent().addClass("active");
            $($(".tab-box>ul>li")[$(boxes[i]).index()]).click();
            $(menuItemLink).click();
            var sidebarScroll = $(menuItemLink).parents(".sidebar.sidebar-scroll");
            $(menuItemLink).parents(".sidebar.sidebar-scroll").animate({
                scrollTop: $(sidebarScroll).scrollTop() + $(menuItemLink).parent().offset().top - $(sidebarScroll).offset().top
            }, 1000);

            if (action && action !== null && action !== "") {
                if (action === "create") {
                    var container = $("div.bodyDiv", restrictedArea).not(":has(div.tab-container)");
                    var url = "/console/rest/html/" + model + "/" + action + searchUrl.substring(searchUrl.indexOf("?"));
                    qz.fill(url, {}, ($(container).length > 0 ? container : $(".main-body", restrictedArea).not(":has(div.tab-container)")), false, null);
                }
            }
            $(".nav-tabs a[tabGroup='" + group + "']", restrictedArea).click();
            var count = 12;
            var interval = window.setInterval(function () {
                if (count > 0) {
                    var targetEle = $("label[for='" + field + "']", restrictedArea);
                    if ($(targetEle).length > 0) {
                        window.clearInterval(interval);
                        if ($(targetEle).is(":visible")) {
                            var scrollEle = $(".main-body", restrictedArea);
                            $(scrollEle).animate({scrollTop: $(scrollEle).scrollTop() + $(targetEle).offset().top - $(scrollEle).offset().top} - 20, 1000);
                            shaking($(targetEle)[0]);
                        } else {
                            shakeTip(getSetting("searchHiddenTip"));
                        }
                    }
                    count--;
                } else {
                    window.clearInterval(interval);
                }
            }, 50);
            return;
        }
    }
};

function shaking(el) {
    var maxDistance = 5; // 抖动偏移距离
    var interval = 12; // 抖动快慢，数字越小越快，太小DOM反应不过来，看不出动画
    var quarterCycle = 36; // 一次完整来回抖动的四分之一周期
    var curDistance = 0;
    var direction = 1;
    var timer = setInterval(function () {
        if (direction > 0) {
            curDistance++;
            if (curDistance === maxDistance) {
                direction = -1;
            }
        } else {
            curDistance--;
            if (curDistance === -maxDistance) {
                direction = 1;
            }
        }
        el.style.left = curDistance + "px";
        el.style.position = "relative";
    }, interval);
    setTimeout(function () {
        clearInterval(timer);
        el.style.left = "0 px";
        el.style.position = "";
    }, maxDistance * interval * quarterCycle);
}

/**************************************** form.jsp - start *************************************************/
function bindEventForFormPage() {
    // form页面(修改密码动态密码二维码加载)
    $(".form-btn a[btn-type='qrOtp']").unbind("click").bind("click", function (e) {
        e.preventDefault();
        var keyUrl = $(this).attr("href");
        var urlSP = "&";
        if (keyUrl.indexOf("?") <= 0) {
            urlSP = "?";
        }
        var imgSrc = keyUrl + urlSP + new Date().getTime();// 有时候会缓存二维码，刷不到最新的
        var html = "<div style='text-align:center;'>"
            + "<img src='" + imgSrc + "' style='width:200px; height:200px; margin-top:20px; padding:6px;' onerror='this.src=\"./static/images/data-empty.svg\"'>"
            + "<br><div class=\"input-control\" style=\"width: 200px;text-align: center;margin-left: 26%;\">"
            + "<input id=\"randCode-OTP\" type=\"text\" class=\"form-control\" placeholder=\"" + getSetting("placeholderOtp") + "\"></div>"
            + "<label id='verifyCodeOtpError' class=\"qz-error-info\" style=\"position:relative; margin-left:-68px; color:red;\"></label>"
            + "</div>";
        openLayer({
            area: ["450px", "400px"],
            shadeClose: true,
            title: getSetting("layerTitleOtp"),
            content: html,
            yes: function (index) {
                var params = {};
                params[getSetting("checkOtp")] = $.trim($("#randCode-OTP").val());
                $.ajax({
                    url: (imgSrc.substring(0, imgSrc.lastIndexOf("/")) + "/confirmKey").replace("/" + getSetting("downloadView") + "/", "/" + getSetting("jsonView") + "/"),
                    async: true,
                    data: params,
                    dataType: "json",
                    success: function (data) {
                        if (data.success === "true" || data.success === true) {
                            closeLayer(index);
                            showMsg(getSetting("bindSuccessOtp"), data.msg_level);
                        } else {
                            $("#verifyCodeOtpError").html(getSetting("bindFailOtp"));
                        }
                    },
                    error: function (e) {
                        handleError(e);
                    }
                });
            }
        });
        return false;
    });
    // 只读元素鼠标手势
    $(".form-group [readonly]").each(function () {
        $(this).css("cursor", "not-allowed").parent().css("cursor", "not-allowed");
    });
    // 日期组件设置
    $(".form-datetime").datetimepicker({
        weekStart: 1,
        todayBtn: 1,
        autoclose: 1,
        todayHighlight: 1,
        forceParse: 0,
        showMeridian: 1
    });
    //绑定失去焦点数据回显事件
    bindEchoItemEvent();
    bindFormEvent();
};

function bindEchoSelectEvent() {
    //列表搜搜框下拉选级联回显
    var currentform = $("form[name='filterForm']", getRestrictedArea(true));
    var echoGroupElements = currentform.find('input[echogroup], select[echogroup]');
    if (echoGroupElements.length > 0 ) {
        echoGroupElements.each(function () {
            var current = $(this);
            var target;
            if ($(this).prop("tagName").toLowerCase() === "input") {
                target = $(this).parent()
            } else if ($(this).prop("tagName").toLowerCase() === "select") {
                target = $(this)
            }
            target.unbind("change").bind("change", function (e) {
                e.preventDefault();
                var params = {};
                var key = current.next().attr("name");
                params[key] = current.next().attr("value")
                if (current.attr("echogroup") !== undefined && current.attr("echogroup") !== "" && params[key] !== "") { //搜索列表下拉选支持选空，如空则不必调接口
                    echoItem(currentform, params,"","");
                }
            });
        });
    }
}

function bindFormEvent() {
    $("form[name='pageForm'][loaded!='true']").attr("loaded", "true").each(function () {
        var thisForm = $(this);
        // 表单元素级联控制显示/隐藏，只读的事件绑定
        bindEvent(JSON.parse($.trim($("textarea[name='showCondition']", thisForm).val())));
        var passwordFields = $.trim($("textarea[name='passwordFields']", thisForm).val()).split(",");
        // form 表单异步提交(ajax form)
        $(this).ajaxForm({
            type: "POST",
            dataType: "json",
            beforeSerialize: function () {
                var encrypt = new JSEncrypt({"default_key_size": getSetting("encrypt_key_size")});
                encrypt.setPublicKey($.trim($("textarea[name='pubkey']", thisForm).val()));
                for (var i = 0; i < passwordFields.length; i++) {
                    var pwd = $("input[name='" + passwordFields[i] + "']", thisForm).val();
                    $("input[name='" + passwordFields[i] + "']", thisForm).attr("originVal", pwd).val(encrypt.encryptLong2(pwd));
                }
                var names = {};
                $("input[type='checkbox'],select", thisForm).each(function () {
                    if ($(this).attr("name")) {
                        names[$(this).attr("name")] = $(this).prop("tagName");
                    }
                });
                for (var name in names) {
                    if (names[name] === "INPUT") {
                        if ($("input[type='checkbox'][name='" + name + "']:checked", thisForm).length === 0) {
                            $("#tempZone", thisForm).append("<input name='" + name + "' value=''>");
                        }
                    }
                    if (names[name] === "SELECT") {
                        if ($("select[name='" + name + "'] option:selected", thisForm).length === 0) {
                            $("#tempZone", thisForm).append("<input name='" + name + "' value=''>");
                        }
                    }
                }
            },
            beforeSubmit: function (data) {
                $("#mask-loading").show();
                $("input[data-type='password']", thisForm).attr("type", "password");
                $("input[data-type='password']", thisForm).next().find("i").removeClass("icon-eye-open").addClass("icon-eye-close");
                $(".form-btn .btn", thisForm).attr("disabled", true);
                if ($("input[type='file']", thisForm).length > 0) {
                    $(thisForm).attr("enctype", "multipart/form-data");
                } else {
                    $(thisForm).removeAttr("enctype", "multipart/form-data");
                }
                $(".tab-has-error", thisForm).removeClass("tab-has-error");
                $(".form-group .qz-error-info", thisForm).html("");
                $(".has-error", thisForm).removeClass("has-error");
                $("select[multiple='multiple']", thisForm).multipleSelect("refresh");
                return true;
            },
            success: function (data) {
                $("#mask-loading").hide();
                $(".form-btn .btn", thisForm).removeAttr("disabled");
                if (data.success === "true" || data.success === true) {
                    $(".sidebar-menu:visible", getRestrictedArea()).not(":has(div.tab-container)")
                        .find("li.active:visible")
                        .each(function () {
                            if (!$(this).hasClass("menu-open")) {
                                $("a", this).click();
                            }
                        });

                    showMsg(data.msg, data.msg_level);
                } else {
                    $("#tempZone", thisForm).html("");
                    for (var i = 0; i < passwordFields.length; i++) {
                        $("input[name='" + passwordFields[i] + "']", thisForm).val($("input[name='" + passwordFields[i] + "']", thisForm).attr("originVal"));
                    }
                    if (data.data) {
                        var errorData = data.data;
                        for (var key in errorData) {
                            $("#form-item-" + key + " > div:first", thisForm).attr("error-key", key).addClass("has-error");
                            if ($(".nav.nav-tabs", thisForm).length < 1) {
                                $("#form-item-" + key + " > div:first .qz-error-info", thisForm).html(errorData[key]);
                            }
                        }
                        $(".nav.nav-tabs > li", thisForm).each(function (i) {
                            $(this).removeClass("active");
                            $($("a", this).attr("href"), thisForm).removeClass("active");
                            if ($(".has-error", $($("a", this).attr("href")), thisForm).length > 0) {
                                $(this).addClass("tab-has-error");
                            }
                        });
                        $(".nav.nav-tabs > li.tab-has-error", thisForm).each(function (i) {
                            if (i === 0) {
                                $(this).addClass("active");
                                $($("a", this).attr("href"), thisForm).addClass("active");
                            }
                            $(".has-error", $($("a", this).attr("href"), thisForm)).each(function () {
                                $("label.qz-error-info", this).html(errorData[$(this).attr("error-key")]);
                            });
                        });
                        $($("a", $(".nav.nav-tabs > li.tab-has-error", thisForm).first()).attr("href"), thisForm).addClass("active");// TODO 需要考虑多级 Tab 标签。
                        //$("html, body").animate({scrollTop: $(".has-error", thisForm).first().offset().top - 100}, 500);
                    } else {
                        showMsg(data.msg, data.msg_level);
                    }
                }
            },
            error: function (e) {
                handleError(e);
                $(".form-btn .btn", thisForm).removeAttr("disabled");
                for (var i = 0; i < passwordFields.length; i++) {
                    var pwdEle = $("input[name='" + passwordFields[i] + "']", thisForm);
                    $(pwdEle).val($(pwdEle).attr("originVal")).removeAttr("originVal");
                }
            }
        });
    });
};

function bindEchoItemEvent() {
    //查找当前表单下所有回显数据元素，添加失去焦点事件
    var currentform = $("form[name='pageForm']",getRestrictedArea(true));
    var echoGroupElements = currentform.find('[echoGroup]');
    echoGroupElements.each(function () {
        //通过
        var current = $(this);
        var target = $(this);
        //nice-select需拿hidden的input框
        if ($(this).prop("tagName").toLowerCase() === "input" && $(this).attr("type") === "text") {
            target = $(this).closest(".nice-select").find("input[type='hidden']");
        }
        $(target).unbind("change").bind("change", function (e) {
            e.preventDefault();
            var params = $("form[name='pageForm']").formToArray();
            if ($(current).attr("echogroup") !== undefined && $(current).attr("echogroup") !== "") {
                echoItem(currentform, params, $(target).attr("name"), $(current).attr("echogroup"));
            }
        });
    });
}

function echoItem(thisForm, params, item, echoGroup) {
    var isList = true;
    var action = $(thisForm).attr("action");
    action = action.substring(0, action.lastIndexOf("/")) + "/echo";
    var url = action.substring(0, action.lastIndexOf("/"));
    var id;
    if (url.endsWith("update")) {
        id = action.substring(action.lastIndexOf("/") + 1);
        url = url.substring(0, url.lastIndexOf("/"));
    }
    url = url + "/echo" + (id ? "/" + id : "");
    url = url.replace(getSetting("htmlView"), getSetting("jsonView"));
    var submitValue = params;
    if (item !== "" && echoGroup !== "") {
        let bindNames = new Set();
        $(thisForm).find('[echoGroup]').each(function () {
            for (let group of echoGroup.split(",")) {
                if ($(this).attr("echogroup").split(",").includes(group)) {
                    if ($(this).attr("name") === undefined){
                        //nice-select需拿hidden的input框的值
                        bindNames.add($(this).closest(".nice-select").find("input[type='hidden']").attr("name"))
                    }else{
                        bindNames.add($(this).attr("name"));
                    }
                }
            }
        });
        submitValue = params.filter(item => bindNames.has(item.name));
        isList = false;
    }

    $.post(url, submitValue, function (data) {
        updateFormData(thisForm, data.data, data.options, isList);
    }, "json");
}

function getI18n(i18nArr){
    var flag = getSetting("langFlag")
    for (let item of i18nArr) {
        if (item.startsWith(flag+":")){
            return item.split(":")[1]
        }
    }
    return i18nArr[0];
}

function updateFormData(thisForm, data, options, isList) {
    for (let option of options){
        var formItem;
        if (isList){
            formItem = $("#form-item-" + option.field, thisForm);
        }else{
            formItem = $("#form-item-" + option.field + " > div:first", thisForm);
        }
        var type = formItem.attr("type");
        let html = "";
        let echoGroup = "";
        switch (type) {
            case "multiselect":
                echoGroup = $(formItem).find("select[name='" + option.field + "']").attr("echogroup");
                //获取placeholder
                let placeholder = $("select",formItem).attr("placeholder");
                //渲染原始html
                html += "<select echoGroup=\"+ echoGroup +\" name=\""+ option.field +"\" multiple=\"multiple\" style=\"width:100%;\"\n" +
                    "        placeholder=\""+ placeholder +"\">"
                for (let op of option.options) {
                    if (op.name === option.value){
                        html += "<option value=\"" + op.name.replace(/"/g, '&quot;') + "\" selected>\n"+ getI18n(op.i18n) +"</option>";
                    }else{
                        html += "<option value=\"" + op.name.replace(/"/g, '&quot;') + "\">\n"+ getI18n(op.i18n) +"</option>";
                    }
                }
                html += "</select>"
                if (!isList){
                    html += "<label class=\"qz-error-info\"></label>"
                }
                $(formItem).html(html);
                //初始化select
                $("select[multiple='multiple'][loaded!='true']").attr("loaded", "true").each(function () {
                    var $this = $(this);
                    $(this).multipleSelect({
                        locale: getSetting("locale"),
                        multiple: true,
                        multipleWidth: 180,
                        filter: true,
                        filterGroup: true,
                        showClear: true,
                        animate: "fade",
                        onAfterCreate: function () {
                            $("ul li.multiple", $($this).next(".ms-parent")).each(function () {
                                $(this).attr("title", $("label>span", this).first().text());
                                var name = $("label>input", this).first().val();
                                var color = $(this).parent().parent().parent().siblings("select[multiple='multiple']").find("option[value=" + name + "]").css("color")
                                $("label>span", this).css("color", color);
                                $(this).hover(function () {
                                    $("label>span", this).css({"white-space": "normal"});
                                }, function () {
                                    $("label>span", this).css({"white-space": "nowrap"});
                                });
                            });
                            return true;
                        }
                    });
                });
                break;
            case "select":
                //渲染列表
                html = "<li data-value=\"\" class=\"option focus\" format=\"\"></li>"
                for(let op of option.options){
                    //todo 国际化
                    html += "<li data-value=\""+ op.name.replace(/"/g, '&quot;') +"\" class=\"option\" format=\""+ op.name.replace(/"/g, '&quot;') +"\">"+ getI18n(op.i18n) +"</li>"
                }
                $("ul",formItem).html(html);
                //渲染选中
                var $li = $("li[data-value='" + option.value + "']", formItem);
                if ($li.length > 0) {
                    $li.each(function (index,ele){
                        selectOption.call(ele,false);
                    });
                } else {
                    $("input[type='hidden']", formItem).val(option.value);
                    $("input[type='hidden']", formItem).attr("format", option.value);
                    $("input[type='text']", formItem).attr("text", option.value).val(option.value);
                    $("div.nice-select span", formItem).html(option.value);
                }
                break;
            case "sortable_checkbox":
                //渲染页面
                for(let op of option.options){
                    html += "<a draggable=\"true\" href=\"javascript:void(0);\">\n" +
                        "        <input type=\"checkbox\" name=\""+ option.field +"\" value=\""+ op.name.replace(/"/g, '&quot;') +"\">\n" +
                        "        <label>"+ getI18n(op.i18n) +"\n" +
                        "        </label>\n" +
                        "    </a>";
                }
                $("div",formItem).html(html);
                //选中
                $("a", formItem).each(function () {
                    var val = $("input[name=" + option.field + "]", this).attr("value");
                    if (option.value.split(",").includes(val)) {
                        $("input[name=" + option.field + "]", this).prop("checked", true);
                    } else {
                        $("input[name=" + option.field + "]", this).prop("checked", false);
                    }
                });
                break;
            case "checkbox":
                //获取echoGroup
                echoGroup = $(formItem).find("input[name='" + option.field + "']").attr("echogroup");
                //渲染页面
                for(let op of option.options){
                    html += "<label class=\"checkbox-inline checkbox-label checkbox-anim\">\n" +
                        "    <input echoGroup=\""+ echoGroup +"\" type=\"checkbox\" name=\""+ option.field +"\" value=\""+ op.name.replace(/"/g, '&quot;') +"\">\n" +
                        "    <i class=\"checkbox-i\"></i> "+ getI18n(op.i18n) +"\n" +
                        "</label>"
                }
                if (!isList){
                    html += "<label class=\"qz-error-info\"></label>"
                }
                $(formItem).html(html);
                //选中
                $(formItem).find("input[name='" + option.field + "']").each(function () {
                    if ($(this).attr("value") !== option.value) {
                        $(this).prop("checked", false);
                    } else {
                        $(this).prop("checked", true);
                    }
                });
                break;
            case "radio":
                //获取echoGroup
                echoGroup = $(formItem).find("input[name='" + option.field + "']").attr("echogroup");
                for(let op of option.options){
                    html += "<label class=\"radio-inline radio-label radio-anim\">\n" +
                        "    <input type=\"radio\" name=\""+ option.field +"\" value=\""+ op.name.replace(/"/g, '&quot;') +"\" echogroup=\""+ echoGroup +"\">\n" +
                        "    <i class=\"radio-i\"></i> "+ getI18n(op.i18n) +"\n" +
                        "</label>";
                }
                if (!isList){
                    html += "<label class=\"qz-error-info\"></label>"
                }
                $(formItem).html(html);
                //选中
                $(formItem).find("input[name='" + option.field + "']").each(function () {
                    if ($(this).attr("value") !== option.value) {
                        $(this).prop("checked", false);
                    } else {
                        $(this).prop("checked", true);
                    }
                });
                break;
        }
    }
    //因为重新渲染了option，所以需要重新绑定echoData
    if (isList){
        bindEchoSelectEvent()
    }else{
        bindEchoItemEvent();
    }
    for (let key in data) {
        var value = data[key];
        var formItem = $("#form-item-" + key + " > div:first", thisForm);
        var type = formItem.attr("type");
        switch (type) {
            case "bool":
                var val = $("input[name='" + key + "']", formItem).val();
                if (val !== value) {
                    $("div.switch-btn", formItem).trigger("click");
                }
                break;
            case "checkbox":
            case "radio":
                $(formItem).find("input[name='" + key + "']").each(function () {
                    if ($(this).attr("value") !== value) {
                        $(this).prop("checked", false);
                    } else {
                        $(this).prop("checked", true);
                    }
                });
                break;
            case "select":
                var $li = $("li[data-value='" + value + "']", formItem);
                if ($li.length > 0) {
                    $li.each(function (index,ele){
                        selectOption.call(ele,false);
                    });
                } else {
                    $("input[type='hidden']", formItem).val(value);
                    $("input[type='hidden']", formItem).attr("format", value);
                    $("input[type='text']", formItem).attr("text", value).val(value);
                    $("input[type='hidden']", formItem).change();
                    $("div.nice-select span", formItem).html(value);
                }
                break;
            case "sortable_checkbox":
                $("a", formItem).each(function () {
                    var val = $("input[name=" + key + "]", this).attr("value");
                    if (value.split(",").includes(val)) {
                        $("input[name=" + key + "]", this).prop("checked", true);
                    } else {
                        $("input[name=" + key + "]", this).prop("checked", false);
                    }
                });
                break;
            case "sortable":
                $("input[name='" + key + "']", formItem).val(value);
                $('ul.sortable li:not(:first)', formItem).remove();
                var valArr = value.split(",");
                var ulEl = $("ul.sortable", formItem);
                var firstLi = ulEl.find('li:first');
                for (let i = 0; i < valArr.length; i++) {
                    if (i === 0) {
                        $("td.editable label", firstLi).text(valArr[i]);
                    } else {
                        var clonedLi = firstLi.clone();
                        $("td.editable label", clonedLi).text(valArr[i]);
                        ulEl.append(clonedLi);
                    }
                }
                break;
            case "kv":
                $("tbody tr:not(:first,:last)", formItem).remove();
                var alink = $("tbody tr:last td a", formItem);
                var separator = $(formItem).children("div").attr("separator");
                if (value !== null && value !== '') {
                    var valArr = value.split(separator);
                    for (let val of valArr) {
                        var arr = val.split("=");
                        addDictRow(alink, false, arr[0], arr[1]);
                    }
                }
                break;
            case "textarea":
                $("textarea[name='" + key + "']", formItem).val(value);
                break;
            default:
                $("input[name='" + key + "']", formItem).val(value);
        }
    }
}

/**************************************** form.jsp - end *************************************************/

/**************************************** sortable.jsp - start *************************************************/
function addRow(alink, readonly) {
    if (readonly) {
        return;
    }
    var ulEle = $("ul.sortable", $(alink).parents(".form-control-sortable"));
    var selectedRow = $("li[selected='selected']", ulEle);
    var html = "<li class=\"droptarget\" draggable=\"true\"><table class=\"table table-bordered dragtable\">"
        + "<tr><td class=\"editable\" style=\"padding:0px 0px !important;\"><label></label></td>"
        + "<td class=\"narrow\"><a href=\"javascript:void(0);\" class=\"editable-edit\""
        + "onclick=\"bindEditable(this, false);\"><i class=\"icon icon-edit\"></i></a></td>"
        + "<td class=\"narrow\"><a href=\"javascript:void(0);\" onclick=\"removeRow(this, false);\"><i class=\"icon icon-trash\"></i></a></td>"
        + "<td class=\"draggable narrow\"><a href=\"javascript:void(0);\"><i class=\"icon icon-arrows\"></i></a>\</td>"
        + "</tr></table></li>";
    if (selectedRow.length > 0) {
        $(selectedRow).before(html);
    } else {
        $(ulEle).append(html);
    }
    refreshTable();
};

function removeRow(aObj, readonly) {
    if (!readonly) {
        $($(aObj).parents("li")[0]).remove();
        refreshTable();
    }
};

function bindEditable(linkObj, readonly) {
    if (readonly) {
        return;
    }
    $("label", $(linkObj).parent().prev()).hide();
    $("label", $(linkObj).parent().prev()).after("<input type=\"text\" class=\"form-control\" value=\"" + $.trim($("label", $(linkObj).parent().prev()).html()) + "\" />");
    var inputEle = $("input", $(linkObj).parent().prev());
    $(inputEle).keydown(function (e) {
        if (e.which === 13) {
            $(this).blur();
            return false;
        }
    }).blur(function () {
        $("label", $(linkObj).parent().prev()).text($.trim($(this).val())).show();
        $(linkObj).parent().parent().prev().css({"background-color": ""}).removeAttr("selected");
        $(this).remove();
        refreshTable();
    });
    // 解决获取焦点时光标不在末尾的问题
    var val = $(inputEle).val();
    $(inputEle).val("");
    $(inputEle).focus();
    $(inputEle).val(val);
};

function refreshTable() {
    $("ul.sortable").each(function () {
        var value = "";
        $("li", this).each(function (i) {
            var tdVal = $.trim($($("td", this).first()).text());
            if (tdVal !== "") {
                value += tdVal + $(this).parent().parent().attr("separator");
            }
        });
        $("input[type='hidden']", $(this).parent()).val(value !== "" ? value.substring(0, value.length - 1) : value);
        $("input[type='hidden']", $(this).parent()).change();
        $("li .editable", this).first().unbind("click").click(function () {
            var row = $(this).parents("li");
            if (row.attr("selected") === "selected") {
                row.css({"background-color": ""}).removeAttr("selected");
            } else {
                $("li[selected='selected']", $(this).parents("ul")).css({"background-color": ""}).removeAttr("selected");
                row.attr("selected", "selected").css({"background-color": "#ebf2f9"});
            }
        });
    });
};

function dragable() {
    var dragging = null;
    $(".form-control-sortable>ul.sortable").unbind("selectstart,dragstart,drag,dragend,dragenter,dragover,dragleave,drop")
        .bind("selectstart", function (e) {
            e.preventDefault();
            return false;
        }).bind("dragstart", function (e) {
        dragging = $(e.target).parents("li.droptarget")[0];
    }).bind("dragover", function (e) {
        e.preventDefault();
        var target = $(e.target).parents("li.droptarget");
        if (target.length > 0 && target[0] !== dragging) {
            if ($(target[0]).index() < $(dragging).index()) {
                $(target[0]).before(dragging);
            } else {
                $(target[0]).after(dragging);
            }
            refreshTable();
        }
    }).bind("drop", function (e) {
        e.preventDefault();
    });
};

/**************************************** sortable.jsp - end *************************************************/
/**************************************** kv.jsp - start *************************************************/
function addDictRow(alink, readonly, key, value) {
    if (!readonly) {
        var tr = $(alink).parent().parent();
        var html = "<tr>"
            + "<td class=\"edit-kv\" style=\"padding:0px 0px !important;\"><input type=\"text\" class=\"form-control\" value='" + (key ? key : '') + "' onchange=\"refreshDict()\" /></td>"
            + "<td class=\"edit-kv\" style=\"padding:0px 0px !important;\"><input type=\"text\" class=\"form-control\" value='" + (value ? value : "") + "' onchange=\"refreshDict()\" /></td>"
            + "<td class=\"narrow\"><a href=\"javascript:void(0);\" onclick=\"removeDictRow(this, false);\"><i class=\"icon icon-trash\"></i></a></td>"
            + "</tr>";
        $(tr).before(html);
        refreshDict();
    }
};

function removeDictRow(aObj, readonly) {
    if (!readonly) {
        $(aObj).parent().parent().remove();
        refreshDict();
    }
};

function refreshDict() {
    $("table.kv").each(function () {
        var value = "";
        $("tr", this).each(function () {
            var trInputs = $("input", this);
            if (trInputs.length === 2) {
                var entry = $.trim($(trInputs[0]).val()) + "=" + $(trInputs[1]).val();
                value += entry + $(this).parent().parent().parent().attr("separator");
            }
        });
        value = value !== "" ? base64Encode(value.substring(0, value.length - 1)) : value;
        $("input[type='hidden']", $(this).parent()).val(value);
        $("input[type='hidden']", $(this).parent()).change();
    });
};

/**************************************** kv.jsp - end *************************************************/
/**************************************** sortablecheckbox.jsp - start *************************************************/
function checkboxSortable() {
    $(".checkbox-group a").unbind("click").bind("click", function () {
        $("input", this).prop("checked", !$("input:not([readonly])", this).prop("checked"));
        if ($(this).parent().is(".sortable")) {
            $("label", this).css({"cursor": ($("input", this).is(":checked")) ? "move" : "default"});
        }
    });
    var draging = null;
    $(".checkbox-group a input:checked:not([readonly])").next().css({"cursor": "move"});
    $(".checkbox-group.sortable").unbind("selectstart,dragstart,drag,dragend,dragenter,dragover,dragleave,drop")
        .bind("selectstart", function (e) {
            e.preventDefault();
            return false;
        }).bind("dragstart", function (e) {
        draging = e.target;
    }).bind("dragover", function (e) {
        e.preventDefault();
        var target = $(e.target).parents("a[draggable='true']")[0];
        if ($(e.target).parents(".checkbox-group").length > 0 && target !== draging) {
            if ($(target).index() < $(draging).index()) {
                $(target).before(draging);
            } else {
                $(target).after(draging);
            }
        }
        return false;
    });
};
/**************************************** sortablecheckbox.jsp - end *************************************************/

/**************************************** list.jsp - start *************************************************/
    // 列表页表格操作列特定事件绑定
var bindingActions = {
        "action_list": function (dom, selector, restrictedArea) {// 列表页表格操作列(启动、停止、应用卸载、删除等)
            $(dom).attr("loaded", "true").bind("click", function (e) {
                e.preventDefault();
                var actUrl = $(dom).attr("href");
                var actionId = $(dom).attr("action-id");
                var bindId = $(dom).attr("data-id");
                var filterForm = $("form[name='filterForm']");
                showConfirm($(dom).attr("act-confirm"), {
                    "title": getSetting("pageConfirmTitle"),
                    "btn": [getSetting("confirmBtnText"), getSetting("cancelBtnText")]
                }, function (index) {
                    if (getSetting("actionId_app_stop-delete").indexOf(actionId) >= 0) {
                        $("label.close").each(function () {
                            if ($(this).parent().attr("bind-id") === bindId) {
                                $(this).click();
                            }
                        });
                    }
                    closeLayer(index);
                    confirm_method(filterForm, actUrl);
                });
                return false;
            });
        },
        "download": function (dom, selector, restrictedArea) {// 列表页表格操作列及form页面(下载日志、快照等)
            $(selector + "[loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function (e) {
                e.preventDefault();
                if ($(this).attr("href") !== "#" && $(this).attr("href").indexOf("javascript:") < 0) {
                    downloadFiles($(this).attr("href"), $(this).attr("downloadfile"));
                }
                return false;
            });
        },
        "sub_form": function (dom, selector, restrictedArea) {
            $(selector + "[loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function (e) {
                e.preventDefault();
                if ($(this).attr("href") !== "#" && $(this).attr("href").indexOf("javascript:") < 0) {
                    popupAction($(this).attr("href"), $(this).attr("action-id"), $(this).attr("data-tip"), $(this).closest("section.main-body"), $(this).attr("form-loaded-trigger"),$(this).attr("get-data-url"));
                }
                return false;
            });
        },
        "sub_menu": function (dom, selector, restrictedArea) {
            $(selector + "[loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function (e) {
                e.preventDefault();
                openTab($(this).attr("href"), $(this).attr("href"), $(this).attr("data-name"));
                return false;
            });
        }
    };

/**
 * 列表页面相关事件绑定
 */
function bindEventForListPage() {
    // 初始化分页条
    $("ul.pager.pager-loose[loaded!='true']").attr("loaded", "true").each(function () {
        var partLinkUri = $(this).attr("partLinkUri");
        $(this).pager({
            page: Number($(this).attr("data-page")),
            recPerPage: Number($(this).attr("recPerPage")),
            recTotal: Number($(this).attr("data-rec-total")),
            linkCreator: function (page, pager) {
                var uri = partLinkUri + page;
                return encodeURI(uri);
            },
            lang: getSetting("pageLang")
        });
    });

    bindEchoSelectEvent();

    // 列表搜索框回车
    $("form[name='filterForm'][loaded!='true']").attr("loaded", "true").unbind("keypress").bind("keypress", function (e) {
        if (e.keyCode === 13) {
            e.preventDefault();
            qz.fill($("a", this).first().attr("href"), qz.formToJson(this), $(this).closest(".main-body"), false, null);
        }
    });

    $("section.main-body", document.body).each(function () {
        var domSelector = "section.main-body[bindingId='" + $(this).attr("bindingId") + "']";
        var restrictedArea = $(this).parent();
        var containSubTab = $(".tab-wrapper", restrictedArea).length > 0;
        // 搜索按钮
        qz.bindFill(domSelector + " .search-btn a" + (containSubTab ? ":not(div.tab-container a)" : ""), domSelector, false, false, restrictedArea, null);
        // 列表页表格顶部操作按钮
        qz.bindFill(domSelector + " .tools-group a:not([act-confirm]" + (containSubTab ? ", div.tab-container a" : "") + ")", domSelector + ">.bodyDiv:first", false, false, restrictedArea, null);
        // 分页(页码及上一页、下一页、首页、尾页等)
        qz.bindFill(domSelector + " ul.pager.pager-loose a" + (containSubTab ? ":not(div.tab-container a)" : ""), domSelector, false, false, restrictedArea, null);
        // 列表页表格单元格操作
        qz.bindFill(domSelector + " table a.dataid" + (containSubTab ? ":not(div.tab-container a)" : ""), domSelector + ">.bodyDiv:first", false, false, restrictedArea, null);
        // 返回按钮
        qz.bindFill(domSelector + " a[action-type='" + getSetting('back') + "']", domSelector + ">.bodyDiv:first", false, false, restrictedArea, null);

        $("table.qz-data-list a.qz-action-link" + (containSubTab ? ":not(div.tab-container a)" : ""), restrictedArea).each(function () {
            var actionTypeMethod = bindingActions[$(this).attr("action-type")];
            var actionIdSelector = "";
            if ($(this).attr("action-id") !== undefined) {
                actionIdSelector = "[action-id='" + $(this).attr("action-id") + "']";
            }
            if (actionTypeMethod) {
                var selector = "table.qz-data-list a.qz-action-link[action-type='" + $(this).attr("action-type") + "']" + actionIdSelector + (containSubTab ? ":not(div.tab-container a)" : "");
                actionTypeMethod.call(null, this, selector, false, restrictedArea);
            } else {
                if ($(this).attr("action-id") === getSetting("actionId_app_manage")) {// 集群实例点击[管理]，打开新 Tab 并切换
                    $("table.qz-data-list a.qz-action-link" + actionIdSelector + "[loaded!='true']" + (containSubTab ? ":not(div.tab-container a)" : ""), restrictedArea)
                    .attr("loaded", "true").bind("click", function (e) {
                        e.preventDefault();
                        var tab = $(".tab-box>ul>li[bind-id='" + $(this).attr("data-id") + "']");
                        if (tab.length > 0) {
                            tab.click();
                            return;
                        }
                        return initializeManager($(this), $(this).attr("href"));
                    });
                } else {
                    if ($(this).attr("action-type")) {
                        // 列表页表格操作列(【注意】：此行需要后置于具体操作列的事件绑定，否则具体操作列的事件绑定将失效)
                        var selector = "table.qz-data-list a.qz-action-link[action-type='" + $(this).attr("action-type") + "'][action-id!='" + getSetting("actionId_app_manage") + "']" + actionIdSelector
                         + (containSubTab ? ":not(div.tab-container a)" : "");
                        qz.bindFill(selector, domSelector, false, false, restrictedArea, null);
                    } else {
                        console.error("Element binding action failed. Element html:" + $(this)[0].outerHTML);
                    }
                }
            }
        });

        $(".qz-list-operate a[act-confirm][loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function (e) {
            e.preventDefault();
            var dom = this;
            var idSeparator = $(this).attr("id-separa");
            showConfirm($(this).attr("act-confirm"), {
                "title": getSetting("pageConfirmTitle"),
                "btn": [getSetting("confirmBtnText"), getSetting("cancelBtnText")]
            }, function (index) {
                var params = "";
                $("table.list-table[binding='" + $(dom).attr("binding") + "'] input[type='checkbox'][class='morecheck']", restrictedArea).each(function () {
                    if ($(this).prop("checked") && $(this).attr("value") !== undefined && $(this).attr("value") !== null && $(this).attr("value") !== "") {
                        params = params + $(this).attr("value") + idSeparator;
                    }
                });
                var data = {};
                data[$(dom).attr("id-name")] = params;
                $.post($(dom).attr("href"), data, function (data) {
                    closeLayer(index);
                    if (data.success === "true") {
                        $(dom).closest("div.bodyDiv").find("form[name='filterForm']:first").find("a.filter_search").click();
                    } else {
                        showMsg(data.msg, "error");
                    }
                }, "json");
            });
            return false;
        });
    });

    //列表修改值
    $('table .switch-btn, table .input-class, table .nice-select').each(function () {
        var that = this;
        if ($(this).attr("class").includes("switch")) {
            $(this).unbind("click").bind("click", function () {
                var thText = $(this).closest('table').find('thead th').eq($(this).closest('td').index()).text();
                var confirmDetail = getSetting("switchText");
                if (thText !== undefined){
                    confirmDetail += thText;
                }
                showConfirm(confirmDetail, {
                    "title": getSetting("pageConfirmTitle"),
                    "btn": [getSetting("confirmBtnText"), getSetting("cancelBtnText")]
                }, function (index) {
                    $(".switchedge", that).toggleClass("switch-bg");
                    $(".circle", that).toggleClass("switch-right");
                    $("input", that).val($("input", that).val() === "true" ? false : true);
                    closeLayer(index);
                    updateListValue(null, $("input", $(that)));
                }, function () {
                });
            });
        } else {
            $("input", $(this)).bind("change", function (event) {
                updateListValue(event, this);
            });
        }
    });

    function updateListValue(even, target) {
        let fieldStr = $(target).attr("name");
        let v = $(target).val();
        let tempUrl = $(target).closest('td').attr("action");
        if (tempUrl === undefined || tempUrl === "") {
            return;
        }
        let resData;
        if (Array.isArray(v)) {
            resData = [{}];
            v.forEach(function (currentV) {
                resData.push({"name": fieldStr, "value": currentV});
            });
        } else {
            resData = {};
            resData[fieldStr] = v;
        }
        $.ajax({
            type: "POST",
            url: tempUrl,
            data: resData,
            success: function (data) {
            },
            error: function (e) {
                handleError(e);
            }
        });
    }

    $("select[multiple='multiple']").on("change", function (event) {
        updateListValue(event, this);
    });
}

//返回列表页面
function returnHref(backDom) {
    var sidebar = $(backDom).closest("section.main-body").prev();
    var menuLink = $(".treeview.active:last > a", sidebar);
    $(menuLink).trigger("click");
    //$(".menu-open", sidebar).removeClass("menu-open").find(".treeview-menu").hide();
    $(menuLink).parent().parents("li.treeview.active").removeClass("active").addClass("menu-open").find(".treeview-menu");
    $(menuLink).parent().parents("li.treeview.menu-open").children(".treeview-menu").show();
}

function initializeManager(element, url) {
    var tabHtml = "<li id=\"" + new Date().getTime() + "\" bind-id=\"" + element.attr("data-id") + "\">"
        + "<a href=\"javascript:void(0);\" href-attr=\"" + url + "\" rel=\"noopener noreferrer\">"
        + "    <i class=\"icon icon-" + element.attr("model-icon") + "\"></i>"
        + "    <label>" + element.attr("data-name") + "</label>"
        + "</a>"
        + "<label class=\"close\">"
        + "    <i class=\"icon icon-times\"></i>"
        + "</label>"
        + "</li>";
    $(".tab-box>ul").append(tabHtml);
    $(".content-box>ul").append("<li></li>");
    bindTabEvent();
    qz.fill(url, {}, $(".content-box>ul>li").last(), false, function () {
        $("ul[data-widget='tree']", $(".content-box>ul>li").last()).menuTree();
        $("[data-toggle='push-menu']", $(".content-box>ul>li").last()).pushMenu({});
        $(".tab-box>ul>li.active").removeClass("active").addClass("inactive");
        $(".content-box>ul>li.active").removeClass("active").addClass("inactive");
        $(".tab-box>ul>li").last().removeClass("inactive").addClass("active");
        $(".content-box>ul>li").last().removeClass("inactive").addClass("active");
        var firstMenu = $(".sidebar-menu li a[modelname]", $(".content-box>ul>li").last()).first();
        if (firstMenu.length > 0) {
            //$(".sidebar-menu li", $(".content-box>ul>li").last()).removeClass("active");
            //$(".sidebar-menu li.treeview.menu-open", $(".content-box>ul>li").last()).removeClass("menu-open");
            $(firstMenu).parent().addClass("active");
            //$(firstMenu).parents(".treeview-menu").show();
            $(firstMenu).parents(".treeview-menu").each(function() {
                $(this).show().parent(".treeview").addClass("menu-open");
            });
            $(firstMenu).click();
        }
    });    
    return false;
}

/**
 * @param url
 */
function confirm_method(filterForm, url) {
    $("#mask-loading").show();
    $.ajax({
        type: "POST",
        url: url,
        async: true,
        dataType: "json",
        complete: function () {
            $("#mask-loading").hide();
        },
        success: function (data) {
            if (data.success === "true" || data.success === true) {
                var searchBtn = $(".filter_search", getRestrictedArea());
                if (searchBtn.length > 0) {
                    searchBtn.trigger('click'); //点击搜索按钮，请求list
                } else {
                    $("li.treeview.active", getRestrictedArea()).find("a").trigger('click');//点击当前所在菜单，请求list
                }
            }
            showMsg(data.msg, data.msg_level);
        },
        error: function (e) {
            handleError(e);
        }
    });
};

/**
 * 下载文件【通用下载，不只是日志文件】
 */
function downloadFiles(fileListUrl, downloadUrl) {
    $.ajax({
        url: fileListUrl,
        async: true,
        dataType: "json",
        success: function (data) {
            var randId = new Date().getTime();
            var html = "<form id='downloadForm-" + randId + "' action='' method='post'>";
            if (data.data) {
                var keys = [];
                var groups = {};
                var defGroup = "defaultGroup-" + randId;
                var attachmentData = data.data;
                for (var key in attachmentData) {
                    var separaIndex = key.indexOf(getSetting("downdloadGroupSepara"));
                    var group = separaIndex > 0 ? key.substring(0, separaIndex) : defGroup;
                    if (!groups[group]) {
                        groups[group] = [];
                        keys.push(group);
                    }
                    groups[group].push({text: attachmentData[key], value: key});
                }
                for (var j = 0; j < keys.length; j++) {
                    if (keys.length > 1) {
                        var groupName = keys[j] === defGroup ? getSetting("downloadCheckAll") : keys[j];
                        html += "<label class='checkbox-inline checkbox-label checkbox-anim'>";
                        html += "<input type='checkbox' name='" + keys[j] + "' value='" + keys[j] + "' skip='true'>";
                        html += "<i class='checkbox-i'></i>" + groupName + "</label><hr style='margin-top:0px; margin-bottom:6px;'>";
                    }
                    html += "<div class='form-group' style='margin-bottom:18px;'>";
                    for (var i = 0; i < groups[keys[j]].length; i++) {
                        html += "<label class='checkbox-inline checkbox-label checkbox-anim'>";
                        html += "<input type='checkbox' name='" + keys[j] + "' value='" + groups[keys[j]][i].value + "' skip='false'>";
                        html += "<i class='checkbox-i'></i>" + groups[keys[j]][i].text + "</label>";
                    }
                    html += "</div>";
                }
            }
            html += "<label id='fileErrorMsg-" + randId + "' style='height: 20px; color: red; "
                + (data.success === "true" ? "display: none;'>" : ("display: block;'>" + data.msg)) + "</label>";
            html += "</form>";

            openLayer({
                title: getSetting("downloadTip"),
                content: html,
                btn: [getSetting("confirmBtnText"), getSetting("cancelBtnText")],
                success: function () {
                    $("form#downloadForm-" + randId + " input[type='checkbox'][skip='true']").click(function () {
                        $("form#downloadForm-" + randId + " input[type='checkbox'][skip='false'][name='" + $(this).attr("name") + "']").prop("checked", $(this).prop("checked"));
                    });
                    $("form#downloadForm-" + randId + " input[type='checkbox'][skip='false']").click(function () {
                        var checkedAll = $("form#downloadForm-" + randId + " input[type='checkbox'][skip='false'][name='" + $(this).attr("name") + "']:checked").length ===
                            $("form#downloadForm-" + randId + " input[type='checkbox'][skip='false'][name='" + $(this).attr("name") + "']").length;
                        $("form#downloadForm-" + randId + " input[type='checkbox'][skip='true'][name='" + $(this).attr("name") + "']").prop("checked", checkedAll);
                    });
                },
                yes: function (index) {
                    var checked = $("form#downloadForm-" + randId + " input[type='checkbox'][skip='false']:checked");
                    if (checked.length > 0) {
                        var urlSP = "&";
                        if (downloadUrl.indexOf("?") <= 0) {
                            urlSP = "?";
                        }

                        downloadUrl += urlSP + getSetting("downloadFileNames") + "=";
                        var partUrl = "";
                        $(checked).each(function (i) {
                            partUrl += $(this).val();
                            if (i < (checked.length - 1)) {
                                partUrl += getSetting("DOWNLOAD_FILE_NAMES_SP");
                            }
                        });
                        closeLayer(index);
                        showInfo(getSetting("downloadTaskTip"), 1500);
                        window.location.assign(downloadUrl + encodeURI(partUrl));
                    } else {
                        $("#fileErrorMsg-" + randId).text(getSetting("downloadTip"));
                        $("#fileErrorMsg-" + randId).show();
                    }
                }
            });
        },
        error: function (e) {
            handleError(e);
        }
    });
}

function popupAction(actionUrl, actionId, title, restrictedArea, formLoadedTrigger, getDataUrl) {
    let html = $("div[popup-action-id='" + actionId + "']", restrictedArea).html();
    var formId = actionId + Math.floor(Math.random() * 100);
    html = "<div style='padding: 10px'><form id='" + formId + "' method='post' class='form-horizontal'>" + html + "</form></div>";
    openLayer({
        type: 1,
        shadeClose: true,
        title: title,
        area: ['700px', 'auto'],
        maxHeight: 500,
        content: html,
        success: function (layero, index, that) {
            $(document.getElementById(formId)).on('submit', function (e) {
                e.preventDefault();
                let formData = $(this).serialize();
                $.ajax({
                    type: "POST",
                    url: actionUrl,
                    data: formData,
                    success: function (res, textStatus, xhr) {
                        var $popupForm = $(document.getElementById(formId));
                        $popupForm.nextAll().remove();
                        if (xhr.getResponseHeader("Content-Type") && xhr.getResponseHeader("Content-Type").includes("application/json")) {
                            if ((res.success === false || res.success === 'false') && res.msg) {
                                showMsg(res.msg, 'error');
                            } else if ((res.success === true || res.success === 'true') && res.msg) {
                                showMsg(res.msg, 'info');
                                document.getElementsByClassName("layui-layer-ico layui-layer-close layui-layer-close1")[0].click();
                            } else if (JSON.stringify(res) !== '{}') {
                                $popupForm.after("<hr style=\'margin-top: 4px;\'><pre style='background-color: #333;color: #fff;padding: 10px;'>" + JSON.stringify(res, null, 4) + "</pre>");
                                layer.style(index, {height: '500px'});
                            }
                        } else {
                            $popupForm.after('<hr style=\'margin-top: 4px;\'>' + res);
                            layer.style(index, {height: '500px'});
                        }
                    },
                    error: function (e) {
                        handleError(e);
                    }
                });
            });
            if (getDataUrl) {
                $.ajax({
                    type: "GET",
                    url: getDataUrl,
                    success: function (res) {
                        if (res.success === 'true' || res.success === true) {
                            updateFormData($(document.getElementById(formId)), res.data, []);
                            if (formLoadedTrigger === "true" || formLoadedTrigger === true) {
                                $(document.getElementById(formId)).submit();
                            }
                        } else {
                            showMsg(res.msg, res.msg_level);
                        }
                    },
                    error: function (e) {
                        handleError(e);
                    }
                });
            }
        }
    });
}

/**************************************** list.jsp - end *************************************************/
/**************************************** chart.jsp - start *************************************************/
function initChartPage() {
    $(".bodyDiv>div.infoPage[chart='true'][loaded!='true']").attr("loaded", "true").each(function (i) {
        var thisDiv = $(this);
        var chartOption = defaultOption();
        var myChart = echarts.init($("div[container='chart']", this)[0]);
        myChart.renderFlag = true;
        myChart.on('mouseover', {seriesType: 'line'}, function () {
            myChart.renderFlag = false;
        });
        myChart.on('mouseout', {seriesType: 'line'}, function () {
            myChart.renderFlag = true;
        });
        myChart.legendselect = {};
        myChart.on('legendselectchanged', function (params) {
            myChart.legendselect = params.selected;
        });
        handlerChart(myChart, chartOption, $(thisDiv).attr("data-url"), thisDiv);
    });
};

function handlerChart(chartObj, chartOption, url, restrictedArea) {
    var formData = $(restrictedArea).siblings("form.filterForm").serializeArray();
    $.ajax({
        type: "POST",
        data: formData,
        url: url,
        dataType: 'json',
        success: function (res) {
            if (res.xAxis && res.xAxis.length > 0) {
                var xAxis = res.xAxis;
                var data = res.data;
                var dimensions = ['xValue'];
                var keys = Object.keys(data);
                dimensions = [...dimensions, ...keys];
                chartOption.dataset.dimensions = dimensions;
                chartOption.dataset.sourceHeader = false;

                for (let i = 0; i < xAxis.length; i++) {
                    var model = {};
                    model.xValue = xAxis[i];
                    for (let key in data) {
                        model[key] = data[key][i];
                    }
                    addData(chartObj, chartOption, model, data);
                }
                resizeChart(chartObj, chartOption);
            }
        },
        error: function (e) {
            handleError(e);
        }
    });
}

/**************************************** chart.jsp - end *************************************************/

/**************************************** monitor.jsp - start *************************************************/
function initMonitorPage() {
    var randId = new Date().getTime();
    $(".bodyDiv>div.infoPage[chartMonitor='true'][loaded!='true']").attr("loaded", "true").each(function (i) {
        var thisDiv = $(this);
        var monitorI18nInfo = eval("(" + $("textarea[name='monitorI18nInfo']", thisDiv).val() + ")");
        var chartOption = defaultOption(monitorI18nInfo);
        var myChart = echarts.init($("div[container='chart']", this)[0]);
        myChart.renderFlag = true;
        myChart.on('mouseover', {seriesType: 'line'}, function () {
            myChart.renderFlag = false;
        });
        myChart.on('mouseout', {seriesType: 'line'}, function () {
            myChart.renderFlag = true;
        });
        myChart.legendselect = {};
        myChart.on('legendselectchanged', function (params) {
            myChart.legendselect = params.selected;
        });
        (function (chartObj, option, url, keys, restrictedArea, tempId) {
            $(thisDiv).append("<span id=\"monitor-timer-" + tempId + "\" style=\"display:none;\"></span>");
            var retryOption = {retryLimit: 10};
            window.setTimeout(function fn(retryRemain) {
                if (retryRemain === undefined) {
                    retryRemain = retryOption.retryLimit;
                }
                if (retryRemain > 0 && retryRemain <= retryOption.retryLimit && $("span#monitor-timer-" + tempId).length > 0) {
                    retryOption["retryRemain"] = retryRemain;
                    handler(chartObj, option, url, keys, restrictedArea, retryOption, fn);
                }
            }, 10);
        })(myChart, chartOption, $(thisDiv).attr("data-url"), monitorI18nInfo, thisDiv, randId + i);
    });
};

function defaultOption(infoKv) {
    return {
        width: 'auto',
        title: {text: ''},
        tooltip: {
            trigger: 'axis',
            confine: true,
            formatter: function (params) {
                function getContent(param) {
                    var key = param.seriesName;
                    var name;
                    if (infoKv === undefined || infoKv[key] === undefined) {
                        name = key;
                    } else {
                        name = infoKv[key][0];
                    }
                    var value;
                    if (param.value instanceof Array) {
                        value = param.value[param.encode.y[0]]
                    } else {
                        value = param.value[param.dimensionNames[param.encode.y[0]]];
                    }
                    return name + ": " + value;
                }

                function getMaxSeriesNameWidth(params) {//找到内容最长的，给所有属性设置固定width，避免第二列与第一列数据重叠
                    var maxWidth = 0;
                    for (let i = 0; i < params.length; i++) {
                        let text = getContent(params[i]);
                        let width = text.length * 8;
                        if (width > maxWidth) {
                            maxWidth = width;
                        }
                    }
                    return maxWidth;
                }

                var maxSeriesNameWidth = getMaxSeriesNameWidth(params);

                function getHtml(param) {
                    return '<div style="float: left; width: ' + (maxSeriesNameWidth + 70) + 'px;"><span style="background: ' + param.color + '; width: 11px; height: 11px; border-radius: 11px;float: left; margin: 5px 3px;"></span>' +
                        getContent(param) + '&emsp;&emsp;</div>';
                }

                var res = '<div style="clear: both">';
                for (var i = 0; i < params.length; i++) {
                    res += getHtml(params[i]);
                    if (params.length > 11 && i % 2 == 1) {
                        res += '</div><div style="clear: both">';
                    }
                    if (params.length <= 11) {
                        res += '</div><div style="clear: both">';
                    }
                }
                res += "</div>";
                return res;
            }
        },
        legend: {
            data: [], textStyle: {fontSize: 13}, align: 'auto', top: 30,
            formatter: function (name) {
                if (infoKv !== undefined && infoKv[name] !== undefined) {
                    return infoKv[name][0];
                }
                return name;
            },
            tooltip: {
                show: true,
                trigger: "item",
                formatter: function (option) {
                    if (infoKv !== undefined && infoKv[option.name] !== undefined) {
                        return infoKv[option.name][1];
                    }
                    return option.name;
                }
            }
        },
        series: [],
        grid: {
            left: '3%',
            right: '4%',
            bottom: '2%',
            containLabel: true
        },
        toolbox: {
            feature: {
                saveAsImage: {title: false, show: false}
            },
            left: '95%'
        },
        dataset: {
            dimensions: null,
            source: [],
            sourceHeader: true,
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
        },
        yAxis: {
            type: 'value',
            axisLine: {
                show: true
            }
        }
    };
}

function getDateTime() {
    var myDate = new Date();
    var hours = myDate.getHours() < 10 ? "0" + myDate.getHours() : myDate.getHours();
    var minutes = myDate.getMinutes() < 10 ? "0" + myDate.getMinutes() : myDate.getMinutes();
    var seconds = myDate.getSeconds() < 10 ? "0" + myDate.getSeconds() : myDate.getSeconds();
    return hours + ":" + minutes + ":" + seconds;
}

function handler(chartObj, chartOption, url, keys, restrictedArea, retryOption, timerFn) {
    if (keys && JSON.stringify(keys) !== '{}') {
        var dimensions = ['dataTime'];
        for (let key in keys) {
            dimensions.push(key);
        }
        chartOption.dataset.dimensions = dimensions;
        chartOption.dataset.sourceHeader = false;
    }

    $.ajax({
        type: "GET",
        url: url,
        dataType: 'json',
        complete: function (xhr, status) {
            if (retryOption && timerFn) {
                if (status === "success") {
                    retryOption.retryRemain = retryOption.retryLimit;
                } else {
                    retryOption.retryRemain--;
                }
                window.setTimeout(function () {
                    timerFn(retryOption.retryRemain);
                }, 2000);
            }
        },
        success: function (data) {
            if (data.success === "true" || data.success === true) {
                var monitorData = data.data;
                monitorData.dataTime = getDateTime();
                if (chartOption.dataset.source.length >= 20) {
                    chartOption.dataset.source.shift();
                }
                addData(chartObj, chartOption, monitorData, keys);
                resizeChart(chartObj, chartOption);
                panelUpdate(monitorData, restrictedArea);
            } else {
                showMsg(data.msg, data.msg_level);
            }
        },
        error: function (e) {
            handleError(e);
        }
    });
};

function addData(chartObj, option, model, keys) {
    if (!model || JSON.stringify(model) === '{}') {
        return;
    }

    var valueIsItAnInteger = 'true';
    for (var key in keys) {
        var exist = false;
        for (var seriesKey in option.series) {
            if (option.series[seriesKey].name === key) {
                exist = true;
                break;
            }
        }
        if (!exist) {
            option.legend.data.push(key);
            option.series.push({name: key, type: 'line'});
        }
        var value = model[key];
        if (!(/(^[0-9]\d*$)/.test(value))) {
            valueIsItAnInteger = 'false';
        }
    }

    option.dataset.source.push(model);

    if (valueIsItAnInteger === 'true') {
        option.yAxis.minInterval = 1;
    }
    var hiddenFieldValue = $("input[type='hidden'][name='monitorName']").val();
    if (!option.title || !option.title.text || option.title.text.trim() === '') {
        if (hiddenFieldValue && hiddenFieldValue.trim() !== '') {
            option.title.text = hiddenFieldValue;
        }
    }
}

function resizeChart(chartObj, option) {
    let line_num_each_row = 6;// 图例中每行显示的线条数目
    this.setSeriesAndLegend(option, line_num_each_row);
    this.setGrid(option, line_num_each_row);

    if (chartObj.renderFlag === undefined || chartObj.renderFlag) {
        if (!$.isEmptyObject(chartObj.legendselect)) {
            option.legend.selected = chartObj.legendselect;
        }
        // 更新数据展示
        chartObj.setOption(option, false);
        chartObj.resize();
    }
}

function setSeriesAndLegend(option, line_num_each_row) {
    var seriesData = option.series;

    var newLegendData = [];
    var newSeriesData = [];

    seriesData.forEach((el, index) => {

        // 一行显示个数控制
        if (index % line_num_each_row === 0 && index !== 0) {
            newLegendData.push(""); // 分行
        }
        newLegendData.push(el.name);

        newSeriesData.push(el);
    });

    option.series = newSeriesData;
    option.legend.data = newLegendData;
}

function setGrid(option, line_num_each_row) {
    let legendData = option.legend.data;
    let len = legendData.length;
    var bodyWidth = $(document.body).width();
    var fontSize, height;

    if (bodyWidth <= 768) {
        fontSize = 11;
        height = Math.ceil(len / 2) * 19;
    } else if (bodyWidth <= 1200) {
        fontSize = 11;
        height = Math.ceil(len / 3) * 19;
    } else if (bodyWidth < 1600) {
        fontSize = 12;
        height = Math.ceil(len / 4) * 19;
    } else {
        fontSize = 13;
        height = Math.ceil(len / line_num_each_row) * 19;
    }
    option.legend.height = height + 18;
    option.grid.top = 12 + option.legend.height + option.legend.top;
    option.legend.textStyle.fontsize = fontSize;
}

function panelUpdate(monitorData, restrictedArea) {
    $(".panel-body table tr", restrictedArea).each(function () {
        var key = $($("td", this)[0]).attr("key");
        var fieldName = $($("td", this)[0]).attr("field");
        var text = undefined;

        for (var k in monitorData) {
            if (k != "dataTime") {
                if (monitorData[k] instanceof Array) {
                    if (monitorData[k].length > 0) {
                        text = monitorData[k][0][fieldName];
                    }
                } else if (monitorData[k] instanceof Object) {
                    if (key !== undefined) {
                        if (monitorData[k][key] === undefined) {
                            continue;
                        }
                        text = monitorData[k][key][fieldName];
                    } else {
                        text = monitorData[k][fieldName];
                    }
                } else if (monitorData[k] instanceof String && k == fieldName) {
                    text = monitorData[k];
                } else {
                    continue;
                }
                if (text !== undefined) {
                    $($("td", this)[1]).text(text);
                    break;
                }
            }
        }
    });
};

/**************************************** monitor.jsp - end *************************************************/

function autoAdaptTip() {
    var mainBody = $(".main-body:not(div.tab-container .main-body)", getRestrictedArea());
    if (mainBody.length > 0) {
        var ruler = document.createElement("div");
        ruler.style.fontSize = "13px";
        ruler.style.maxWidth = "320px";
        ruler.style.visibility = "hidden";
        document.body.appendChild(ruler);
        var totalHeight = $(mainBody).prop("scrollHeight");
        var scrollTop = $(mainBody).scrollTop();
        var offsetTop = $(mainBody).offset().top;
        $("form[name='pageForm'] span.tooltips:visible:not(div.tab-container span.tooltips)", mainBody).each(function () {
            ruler.innerHTML = $(this).attr("data-tip");
            var top = scrollTop + $(this).offset().top - offsetTop;
            if ((totalHeight - top - 120) < ruler.offsetHeight) {
                $(this).attr("data-tip-arrow", "top-right");
            } else {
                $(this).attr("data-tip-arrow", "bottom-right");
            }
        });
        document.body.removeChild(ruler);
    }
};

function openTab(dataId, dataUrl, tabTitle) {
    var restrictedArea = getRestrictedArea();
    if ($("div.tab-container", restrictedArea).length === 0) {
        var randId = qz.randomStr(8);
        var html = ""
            + "<div id=\"" + randId + "\" class=\"tab-container\" style=\"width:100%;height:" + $(".main-body", restrictedArea).first().height() + "px;\">"
            + "    <div class=\"tab-container-box\" style=\"height:100%;\"></div>"
            + "</div>";
        $(restrictedArea).append(html);
    }
    var bindId = $("div.tab-container", restrictedArea).first().attr("id");
    if (tabMap[bindId] === undefined) {
        tabMap[bindId] = new qz.tab($("#" + randId + " div.tab-container"));
        tabMap[bindId].init($("#" + randId + ">div.tab-container-box"), function () {
            switchTabView(false, bindId);
        }, function () {
            delete tabMap[bindId];
            $("#" + bindId).remove();
            $("aside.main-sidebar,section.main-body", getRestrictedArea()).not(":has(div.tab-container)").fadeIn("slow");
        });
    }
    var qzTab = tabMap[bindId];
    qzTab.addTab(dataId, tabTitle, "", {}, true, function (tabDom) {
        qz.fill(dataUrl, {}, tabDom, false, function () {
            $("aside.main-sidebar", tabDom).css({"margin-top": "0px"});
            $("aside.main-sidebar .menu-toggle-btn", tabDom).css({"margin-bottom": "0px"});
            $("ul[data-widget='tree']", tabDom).menuTree();
            $("a[data-toggle='push-menu']", tabDom).pushMenu({});
            var firstMenu = $(".sidebar-menu li a[modelname]", tabDom).first();
            if (firstMenu.length > 0) {
                //$(".sidebar-menu li", tabDom).removeClass("active");
                //$(".sidebar-menu li.treeview.menu-open", tabDom).removeClass("menu-open");
                $(firstMenu).parent().addClass("active");
                //$(firstMenu).parents(".treeview-menu").show();
                $(firstMenu).parents(".treeview-menu").each(function() {
                    $(this).show().parent(".treeview").addClass("menu-open");
                });
                $(firstMenu).click();
            }
        });
    });
    switchTabView(true, bindId);
}

function switchTabView(show, id) {
    var restrictedArea = getRestrictedArea();
    if (show) {
        $("aside.main-sidebar,section.main-body", restrictedArea).filter(":not(div.tab-container aside.main-sidebar, div.tab-container section.main-body)").fadeOut("slow");
        $("#" + id, restrictedArea).fadeIn("fast");
    } else {
        $("#" + id, restrictedArea).fadeOut("fast");
        $("aside.main-sidebar,section.main-body", restrictedArea).filter(":not(div.tab-container aside.main-sidebar, div.tab-container section.main-body)").fadeIn("slow");
    }
}

function upload(file, url, id) {
    const formData = new FormData();
    formData.append(id, file);
    $.ajax({
        url: url,
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function (data) {
            if (data.success === "true" || data.success === true) {
                var searchBtn = $(".filter_search", getRestrictedArea());
                if (searchBtn.length > 0) {
                    searchBtn.trigger('click'); //点击搜索按钮，请求list
                } else {
                    $("li.treeview.active", getRestrictedArea()).find("a").trigger('click');//点击当前所在菜单，请求list
                }
            }
            showMsg(data.msg, data.msg_level);
        },
        error: function (e) {
            handleError(e);
        }
    });
}
