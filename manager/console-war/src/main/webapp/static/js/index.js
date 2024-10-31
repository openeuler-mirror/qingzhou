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

// 获取限定区域
function getRestrictedArea() {
    return getActiveTabContent();
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
        const restrictedArea = getRestrictedArea();
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
function setOrReset() {
    // 菜单区域禁止鼠标右键
    $(".main-sidebar").unbind("contextmenu").bind("contextmenu", function (e) {
        e.preventDefault();
        return false;
    });
    // 菜单展示优化
    $("aside.main-sidebar[loaded!='true']").attr("loaded", "true").find(".sidebar-menu>li").hover(function () {
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

    const restrictedArea = getRestrictedArea();
    // 左侧菜单点击菜单事件
    $("aside.main-sidebar", document.body).each(function () {
        qz.bindFill("ul.sidebar-menu a", ".main-body", false, true, $(this).parent(), null);
    });
};

function gotoTarget(model, action, group, field) {
    var boxes = new Array(".content-box>ul>li.active:first", ".content-box>ul>li:eq(1)", ".content-box>ul>li:eq(0)");
    const restrictedArea = getRestrictedArea();// TODO 待调整
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
                    url: (imgSrc.substring(0, imgSrc.lastIndexOf("/")) + "/" + getSetting("confirmKey")).replace("/" + getSetting("download") + "/", "/json/"),
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
                    $(".sidebar-menu", getRestrictedArea()).not(":has(div.tab-container)").find("li.active").each(function () {
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
                            $("#form-item-" + key + " > div", thisForm).attr("error-key", key).addClass("has-error");
                            if ($(".nav.nav-tabs", thisForm).length < 1) {
                                $("#form-item-" + key + " > div .qz-error-info", thisForm).html(errorData[key]);
                            }
                        }
                        $(".nav.nav-tabs > li", thisForm).each(function (i) {
                            $(this).removeClass("active");
                            $($("a", this).attr("href")).removeClass("active");
                            if ($(".has-error", $($("a", this).attr("href"))).length > 0) {
                                $(this).addClass("tab-has-error");
                            }
                        });
                        $(".nav.nav-tabs > li.tab-has-error", thisForm).each(function (i) {
                            if (i === 0) {
                                $(this).addClass("active");
                                $($("a", this).attr("href")).addClass("active");
                            }
                            $(".has-error", $($("a", this).attr("href"))).each(function () {
                                $("label.qz-error-info", this).html(errorData[$(this).attr("error-key")]);
                            });
                        });
                        $($("a", $(".nav.nav-tabs > li.tab-has-error").first()).attr("href")).addClass("active");// TODO 需要考虑多级 Tab 标签。
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
    var echoGroupElements = $("form[name='pageForm']").find('[echoGroup]');
    echoGroupElements.each(function () {
        $(this).bind("change", function (e) {
            e.preventDefault();
            var params = $("form[name='pageForm']").formToArray();
            if ($(this).attr("echoGroup") !== undefined && $(this).attr("echoGroup") !== "") {
                echoItem($("form[name='pageForm']", getRestrictedArea()), params, $(this).attr("name"), $(this).attr("echoGroup"));
            }
        });
    });
}

function echoItem(thisForm, params, item, echoGroup) {
    var action = $(thisForm).attr("action");
    action = action.substring(0, action.lastIndexOf("/")) + "/" + getSetting("echoActionName");
    let bindNames = new Set();
    $(thisForm).find('[echoGroup]').each(function () {
        for (let group of echoGroup.split(",")) {
            if ($(this).attr("echoGroup").split(",").includes(group)) {
                bindNames.add($(this).attr("name"));
            }
        }
    });
    const submitValue = params.filter(item => bindNames.has(item.name));
    $.post(action, submitValue, function (data) {
        updateFormData(thisForm, data.data);
    }, "json");
}


function updateFormData(thisForm, data) {
    for (let key in data) {
        const value = data[key];
        const formItem = $("#form-item-" + key + " > div", thisForm);
        const type = formItem.attr("type")
        switch (type) {
            case "bool":
                const val = $("input[name='" + key + "']", formItem).val();
                if (val !== value) {
                    $("div.switch-btn", formItem).trigger("click");
                }
                break;
            case "checkbox":
            case "radio":
                $(formItem).find("input[name='" + key + "']").each(function () {
                    if ($(this).attr("value") !== value) {
                        $(this).attr("checked", false);
                    } else {
                        $(this).attr("checked", true);
                    }
                });
                break;
            case "select":
                $("li[data-value='" + value + "']", formItem).each(selectOption);
                break;
            case "sortablecheckbox":
                $("a", formItem).each(function () {
                    const val = $("input[name=" + key + "]", this).attr("value");
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
                const valArr = value.split(",");
                const ulEl = $("ul.sortable", formItem);
                const firstLi = ulEl.find('li:first');
                for (let i = 0; i < valArr.length; i++) {
                    if (i === 0) {
                        $("td.editable label", firstLi).text(valArr[i]);
                    } else {
                        const clonedLi = firstLi.clone();
                        $("td.editable label", clonedLi).text(valArr[i]);
                        ulEl.append(clonedLi);
                    }
                }
                break;
            case "kv":
                // $("input[name='" + key + "']", formItem).val(value);
                $("tbody tr:not(:first,:last)", formItem).remove();
                const alink = $("tbody tr:last td a", formItem);
                const separator = $(formItem).children("div").attr("separator");
                if (value !== null && value !== '') {
                    const valArr = value.split(separator);
                    for (let val of valArr) {
                        const arr = val.split("=");
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
        var target = e.target;
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
    
    // 列表搜索框回车
    $("form[name='filterForm'][loaded!='true']").attr("loaded", "true").unbind("keypress").bind("keypress", function (e) {
        if (e.keyCode === 13) {
            e.preventDefault();
            qz.fill($("a", this).first().attr("href"), qz.formToJson(this), $(this).closest(".main-body"), false, null);
        }
    });

    // 列表页表格操作列特定事件绑定
    var bindingActions = {
        "monitor": function(selector, exclude, restrictedArea) {// 列表页表格操作列(监视)
            var target = ".bodyDiv" + (exclude ? ":not(div.tab-container .bodyDiv)" : "");
            qz.bindFill(selector, target, false, false, restrictedArea, null);
        },
        "StartStop": function(selector, exclude, restrictedArea) {// 列表页表格操作列(启动、停止)
            var target = ".bodyDiv" + (exclude ? ":not(div.tab-container .bodyDiv)" : "");
            qz.bindFill(selector, target, false, false, restrictedArea, null);
        },
        "files": function(selector, exclude, restrictedArea) {// 列表页表格操作列及form页面(下载日志、快照等)
            $(selector + "[loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function (e) {
                e.preventDefault();
                if ($(this).attr("href") !== "#" && $(this).attr("href").indexOf("javascript:") < 0) {
                    downloadFiles($(this).attr("href"), $(this).attr("downloadfile"));
                }
                return false;
            });
        },
        "PopLayer": function(selector, exclude, restrictedArea) {
            $(selector + "[loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function (e) {
                e.preventDefault();
                if ($(this).attr("href") !== "#" && $(this).attr("href").indexOf("javascript:") < 0) {
                    customAction($(this).attr("href"), $(this).attr("custom-action-id"), $(this).attr("data-tip"), $(this).closest("section.main-body"));
                }
                return false;
            });
        },
        "ViewHtml": function(selector, exclude, restrictedArea) {
            $(selector + "[loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function (e) {
                alert("TODO");
                e.preventDefault();
                return false;
            });
        },
        "SubTab": function(selector, exclude, restrictedArea) {
            $(selector + "[loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function (e) {
                e.preventDefault();
                openTab($(this).attr("data-id"), $(this).attr("href"), $(this).attr("data-name"));
                return false;
            });
        },
        "NewTab": function(selector, exclude, restrictedArea) {// 集群实例点击[管理]，打开新 Tab 并切换
            $(selector + "[loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function (e) {
                e.preventDefault();
                var tab = $(".tab-box>ul>li[bind-id='" + $(this).attr("data-id") + "']");
                if (tab.length > 0) {
                    tab.click();
                    return;
                }
                return initializeManager($(this), $(this).attr("href"));
            });
        },
        "delete": function(selector, exclude, restrictedArea) {// 列表操作列应用卸载
            $(selector + "[loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function (e) {
                e.preventDefault();
                var actUrl = $(this).attr("href");
                var bindId = $(this).attr("data-id");
                var filterForm = $("form[name='filterForm']");
                showConfirm($(this).attr("act-confirm"), {
                    "title": getSetting("pageConfirmTitle"),
                    "btn": [getSetting("confirmBtnText"), getSetting("cancelBtnText")]
                }, function (index) {
                    if (actUrl.indexOf("app/delete") != -1) {
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
        }
    };

    $("section.main-body", document.body).each(function () {
        var restrictedArea = $(this).parent();
        if ($(".tab-wrapper", this).length > 0) {
            // 搜索按钮
            qz.bindFill(".search-btn a:not(div.tab-container .search-btn a)", ".main-body:not(div.tab-container .main-body)", false, false, restrictedArea, null);
            // 列表页表格顶部操作按钮
            qz.bindFill(".tools-group a:not([act-confirm], div.tab-container .tools-group a)", ".bodyDiv:not(div.tab-container .bodyDiv)", false, false, restrictedArea, null);
            // 分页(页码及上一页、下一页、首页、尾页等)
            qz.bindFill("ul.pager.pager-loose a:not(div.tab-container ul.pager.pager-loose a)", ".main-body:not(div.tab-container .main-body)", false, false, restrictedArea, null);
            // 列表页表格单元格操作
            qz.bindFill("table a.dataid:not(div.tab-container table a.dataid)", ".bodyDiv:not(div.tab-container .bodyDiv)", false, false, restrictedArea, null);

            $("table.qz-data-list a.qz-action-link[data-action!='LINK']", restrictedArea).each(function() {
                var selector = "table.qz-data-list a.qz-action-link[data-action='" + $(this).attr("data-action") + "']:not(div.tab-container a)";
                if (bindingActions[$(this).attr("data-action")]) {
                    bindingActions[$(this).attr("data-action")].call(null, selector, true, restrictedArea);
                } else {
                    console.log("Action function binding failed: function " + $(this).attr("data-action") + " not found.");
                }
            });
            // 列表页表格操作列(【注意】：此行需要后置于具体操作列的事件绑定，否则具体操作列的事件绑定将失效)
            qz.bindFill("table.qz-data-list a.qz-action-link[action-type='" + getSetting("link") + "']:not(div.tab-container a)", ".main-body:not(div.tab-container .main-body)", false, false, restrictedArea, null);
        } else {
            // 搜索按钮
            qz.bindFill(".search-btn a", ".main-body", false, false, restrictedArea, null);
            // 列表页表格顶部操作按钮
            qz.bindFill(".tools-group a:not([act-confirm])", ".bodyDiv", false, false, restrictedArea, null);
            // 分页(页码及上一页、下一页、首页、尾页等)
            qz.bindFill("ul.pager.pager-loose a", ".main-body", false, false, restrictedArea, null);
            // 列表页表格单元格操作
            qz.bindFill("table a.dataid", ".bodyDiv", false, false, restrictedArea, null);
            
            $("table.qz-data-list a.qz-action-link[data-action!='" + getSetting("link") + "']", restrictedArea).each(function() {
                var selector = "table.qz-data-list a.qz-action-link[data-action='" + $(this).attr("data-action") + "']";
                if (bindingActions[$(this).attr("data-action")]) {
                    bindingActions[$(this).attr("data-action")].call(null, selector, false, restrictedArea);
                } else {
                    console.log("Action function binding failed: function " + $(this).attr("data-action") + " not found.");
                }
            });
            // 列表页表格操作列(【注意】：此行需要后置于具体操作列的事件绑定，否则具体操作列的事件绑定将失效)
            qz.bindFill("table.qz-data-list a.qz-action-link[data-action='" + getSetting("link") + "']", ".main-body", false, false, restrictedArea, null);
        }

        $(".qz-list-operate a[act-confirm][loaded!='true']", restrictedArea).attr("loaded", "true").bind("click", function(e) {
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
                $.post($(dom).attr("href"), data, function(data) {
                    closeLayer(index);
                    if (data.success === "true") {
                        $(dom).closest("div.bodyDiv").find("form[name='filterForm']").first().find("a.filter_search").click();
                    } else {
                        showMsg(data.msg, "error");
                    }
                }, "json");
            });
            return false;
        });
    });

    $('table .switch-btn, table .input-class, table .nice-select').each(function () {
        $("input", $(this)).bind("change", updateListValue);
    });

    function updateListValue(e) {
        let fieldStr = $(this).attr("name")
        let v = $(this).val()
        let tempUrl = $(this).closest('tr').find('a[href*="edit"]').attr("href");
        if (tempUrl === undefined) {
            return;
        }
        tempUrl = tempUrl.replace("html", "json").replace("edit", "update");
        let resData
        if (Array.isArray(v)) {
            resData = [{}]
            v.forEach(function (currentV) {
                resData.push({"name": fieldStr, "value": currentV})
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
                //去list页面
                let listUrl = tempUrl.replace("json", "html").replace("update", "list")
                returnHref(listUrl.substring(0, listUrl.lastIndexOf("/")))
            },
            error: function (e) {
                handleError(e);
            }
        });
    }

    $("select[multiple='multiple']").on("change", updateListValue);

}

//返回列表页面
function returnHref(href) {
    $(".content-box li.active a[href='" + href + "']").click()
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
    $(".tab-box>ul>li").last().click();
    qz.fill(url, {}, $(".content-box>ul>li").last(), false, null);
    $("ul[data-widget='tree']", $(".content-box>ul>li").last()).menuTree();
    $("[data-toggle='push-menu']", $(".content-box>ul>li").last()).pushMenu({});
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
                var searchBtn = $(".search-btn a", getRestrictedArea());
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

function customAction(actionUrl, customActionId, title, restrictedArea) {
    let html = $("div[custom-action-id='" + customActionId + "']", restrictedArea).html();
    html = "<div style='padding: 10px'><form id='" + customActionId + "' method='post' class='form-horizontal'>" + html + "</form><hr style='margin-top: 4px;'><div id='custom-action-result' ></div></div>";
    openLayer({
        type: 1,
        shadeClose: true,
        title: title,
        area: ['700px', '500px'],
        content: html,
        success: function () {
            $('#' + customActionId).on('submit', function (e) {
                e.preventDefault();
                let formData = $(this).serialize();
                $.ajax({
                    type: "POST",
                    url: actionUrl,
                    data: formData,
                    success: function (res, textStatus, xhr) {
                        if (xhr.getResponseHeader("Content-Type") && xhr.getResponseHeader("Content-Type").includes("application/json")) {
                            $('#custom-action-result').html("<pre style='background-color: #333;color: #fff;padding: 10px;'>" + JSON.stringify(res, null, 4) + "</pre>");
                        } else {
                            $('#custom-action-result').html(res);
                        }
                    },
                    error: function (e) {
                        handleError(e);
                    }
                });
            });
        }
    });
}

/**************************************** list.jsp - end *************************************************/

/**************************************** monitor.jsp - start *************************************************/
function initMonitorPage() {
    var randId = new Date().getTime();
    $(".bodyDiv>div.infoPage[chartMonitor='true'][loaded!='true']").attr("loaded", "true").each(function (i) {
        var thisDiv = $(this);
        var monitorI18nInfo = eval("(" + $("textarea[name='monitorI18nInfo']", thisDiv).val() + ")");
        var xAxisField = $(this).attr("xAxisField");
        var chartOption = defaultOption(monitorI18nInfo, xAxisField);
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
            var autoRefresh = $(restrictedArea).attr("autoRefresh");
            if ("true" === autoRefresh) {
                window.setTimeout(function fn(retryRemain) {
                    if (retryRemain === undefined) {
                        retryRemain = retryOption.retryLimit;
                    }
                    if (retryRemain > 0 && retryRemain <= retryOption.retryLimit && $("span#monitor-timer-" + tempId).length > 0) {
                        retryOption["retryRemain"] = retryRemain;
                        handler(chartObj, option, url, keys, restrictedArea, retryOption, fn);
                    }
                }, 10);
            } else {
                handler(myChart, chartOption, $(thisDiv).attr("data-url"), monitorI18nInfo, thisDiv, retryOption);
            }
        })(myChart, chartOption, $(thisDiv).attr("data-url"), monitorI18nInfo, thisDiv, randId + i);
    });
};

function defaultOption(infoKv, xAxisField) {
    var dimensions = [];
    if (xAxisField) {
        dimensions.push(xAxisField);
    } else {
        dimensions.push("dataTime");
    }
    for (let key in infoKv) {
        dimensions.push(key);
    }
    return {
        width: 'auto',
        title: {text: ''},
        tooltip: {
            trigger: 'axis',
            confine: true,
            formatter: function (params) {
                function getContent(param) {
                    var key = param.seriesName;
                    var name = infoKv[key][0];
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
                formatter: function (option, a, b, c) {
                    if (infoKv !== undefined) {
                        for (var k in infoKv) {
                            if (infoKv[k][0] == option.name) {
                                return infoKv[k][1];
                            }
                        }
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
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: []
        },
        yAxis: {
            type: 'value',
            axisLine: {
                show: true
            }
        }
    };
};

function handler(chartObj, chartOption, url, keys, restrictedArea, retryOption, timerFn) {
    $.ajax({
        type: "GET",
        url: url,
        dataType: 'json',
        complete: function (xhr, status) {
            if (status === "success") {
                retryOption.retryRemain = retryOption.retryLimit;
            } else {
                retryOption.retryRemain--;
            }
            if (timerFn) {
                window.setTimeout(function () {
                    timerFn(retryOption.retryRemain);
                }, 2000);
            }
        },
        success: function (data) {
            if (data.success === "true" || data.success === true) {
                var monitorData = data.data;
                if (monitorData !== null && JSON.stringify(monitorData) !== '{}') {
                    var models = [{
                        dataTime: function () {
                            var myDate = new Date();
                            var hours = myDate.getHours() < 10 ? "0" + myDate.getHours() : myDate.getHours();
                            var minutes = myDate.getMinutes() < 10 ? "0" + myDate.getMinutes() : myDate.getMinutes();
                            var seconds = myDate.getSeconds() < 10 ? "0" + myDate.getSeconds() : myDate.getSeconds();
                            return hours + ":" + minutes + ":" + seconds;
                        },
                        data: monitorData,
                        models: monitorData
                    }];
                    addData(chartObj, chartOption, models, keys, restrictedArea);
                }
            } else {
                showMsg(data.msg, data.msg_level);
            }
        },
        error: function (e) {
            handleError(e);
        }
    });
};

function addData(chartObj, option, models, keys, restrictedArea) {
    if (models === null) {
        return;
    }
    var len = 20;
    if (option.xAxis.data.length >= len) {
        option.xAxis.data.shift();
    }
    var valueIsItAnInteger = 'true';
    for (var key in keys) {
        var legend = (keys[key] instanceof Array) ? keys[key][0] : keys[key];
        var exist = false;
        for (var seriesKey in option.series) {
            if (option.series[seriesKey].name === legend) {
                exist = true;
                break;
            }
        }
        if (!exist) {
            option.legend.data.push(legend);
            var serie = {name: '', type: '', data: []};
            serie.name = legend;
            serie.type = 'line';
            option.series.push(serie);
        }
    }
    for (var i in models) {
        option.xAxis.data.push(models[i].dataTime.call(null));
        var data = models[i].data;
        for (var key in keys) {
            var value = data[key];
            if (!(/(^[0-9]\d*$)/.test(value))) {
                valueIsItAnInteger = 'false';
            }
            var name = (keys[key] instanceof Array) ? keys[key][0] : keys[key];
            for (var seriesKey in option.series) {
                if (option.series[seriesKey].name === name) {
                    if (option.series[seriesKey].data.length >= len) {
                        option.series[seriesKey].data.shift();
                    }
                    option.series[seriesKey].data.push(value);
                    break;
                }
            }
        }
    }
    if (valueIsItAnInteger === 'true') {
        option.yAxis.minInterval = 1;
    }
    var hiddenFieldValue = $("input[type='hidden'][name='monitorName']").val();
    if (!option.title || !option.title.text || option.title.text.trim() === '') {
        if (hiddenFieldValue && hiddenFieldValue.trim() !== '') {
            option.title.text = hiddenFieldValue;
        }
    }
    let line_num_each_row = 6;// 图例中每行显示的线条数目
    this.setpSeriesAndLegend(option, line_num_each_row);
    this.setGrid(option, line_num_each_row);

    if (chartObj.renderFlag === undefined || chartObj.renderFlag) {
        if (!$.isEmptyObject(chartObj.legendselect)) {
            option.legend.selected = chartObj.legendselect;
        }
        // 更新数据展示
        chartObj.setOption(option, false);
        chartObj.resize();
        panelUpdate(models, restrictedArea);
    }
}

function setpSeriesAndLegend(option, line_num_each_row) {
    let seriesData = option.series;

    let newLegendData = [];
    let newSeriesData = [];

    seriesData.forEach((el, index) => {
        let data = el.data;
        let name = el.name;

        // 一行显示个数控制
        if (index % line_num_each_row === 0 && index !== 0) {
            newLegendData.push(""); // 分行
        }
        newLegendData.push(name);

        newSeriesData.push({
            name: name,
            type: 'line',
            stack: 'Total',
            data: data
        });
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
    if (monitorData.length === 0) {
        return;
    }
    $(".panel-body table tr", restrictedArea).each(function () {
        var key = $($("td", this)[0]).attr("key");
        var fieldName = $($("td", this)[0]).attr("field");
        var text = undefined;

        for (var i in monitorData) {
            for (var k in monitorData[i]) {
                if (k != "dataTime") {
                    if (monitorData[i][k] instanceof Array) {
                        if (monitorData[i][k].length > 0) {
                            text = monitorData[i][k][0][fieldName];
                        }
                    } else if (monitorData[i][k] instanceof Object) {
                        if (key !== undefined) {
                            if (monitorData[i][k][key] === undefined) {
                                continue;
                            }
                            text = monitorData[i][k][key][fieldName];
                        } else {
                            text = monitorData[i][k][fieldName];
                        }
                    } else if (monitorData[i][k] instanceof String && k == fieldName) {
                        text = monitorData[i][k];
                    } else {
                        continue;
                    }
                    if (text !== undefined) {
                        $($("td", this)[1]).text(text);
                        break;
                    }
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

var tabMap = {};

function openTab(dataId, dataUrl, tabTitle) {
    var restrictedArea = getRestrictedArea();
    if ($("div.tab-container", restrictedArea).length === 0) {
        var randId = qz.randomStr(8);
        var html = ""
            + "<div id=\"" + randId + "\" class=\"tab-container\">"
            + "    <div class=\"tab-container-box\"></div>"
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
            $(tabDom).css({"height": $(".main-body", restrictedArea).first().height()});
            $("aside.main-sidebar", tabDom).css({"margin-top": "0px"});
            $("aside.main-sidebar .menu-toggle-btn", tabDom).css({"margin-bottom": "0px"});
            $("ul[data-widget='tree']", tabDom).menuTree();
            $("a[data-toggle='push-menu']", tabDom).pushMenu({});
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
