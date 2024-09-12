+function ($) {
    "use strict";

    var DataKey = "lte.pushmenu";

    var Default = {
        collapseScreenSize: 767,
        expandOnHover: false,
        expandTransitionDelay: 10
    };

    var Selector = {
        collapsed: ".sidebar-collapse",
        open: ".sidebar-open",
        mainSidebar: ".main-sidebar",
        contentWrapper: ".content-wrapper",
        searchInput: ".sidebar-form .form-control",
        button: "[data-toggle='push-menu']",
        mini: ".sidebar-mini",
        expanded: ".sidebar-expanded-on-hover",
        layoutFixed: ".fixed"
    };

    var ClassName = {
        collapsed: "sidebar-collapse",
        open: "sidebar-open",
        mini: "sidebar-mini",
        expanded: "sidebar-expanded-on-hover",
        expandFeature: "sidebar-mini-expand-feature",
        layoutFixed: "fixed"
    };

    var Event = {
        expanded: "expanded.pushMenu",
        collapsed: "collapsed.pushMenu"
    };

    // PushMenu Class Definition
    // =========================
    var PushMenu = function (options) {
        this.options = options;
        this.init();
    };

    PushMenu.prototype.init = function () {
        if (this.options.expandOnHover
                || ($("body").is(Selector.mini + Selector.layoutFixed))) {
            this.expandOnHover();
            $("body").addClass(ClassName.expandFeature);
        }

        $(Selector.contentWrapper).click(function () {
            // Enable hide menu when clicking on the content-wrapper on small screens
            if ($(window).width() <= this.options.collapseScreenSize && $("body").hasClass(ClassName.open)) {
                this.close();
            }
        }.bind(this));

        // __Fix for android devices
        $(Selector.searchInput).click(function (e) {
            e.stopPropagation();
        });
    };

    PushMenu.prototype.toggle = function () {
        var windowWidth = $(window).width();
        var isOpen = !$("body").hasClass(ClassName.collapsed);

        if (windowWidth <= this.options.collapseScreenSize) {
            isOpen = $("body").hasClass(ClassName.open);
        }

        if (!isOpen) {
            var windowWidth = $(window).width();
            if (windowWidth > this.options.collapseScreenSize) {
                $("body").removeClass(ClassName.collapsed);
            } else {
                $("body").addClass(ClassName.open);
            }
        } else {
            var windowWidth = $(window).width();
            if (windowWidth > this.options.collapseScreenSize) {
                $("body").addClass(ClassName.collapsed);
            } else {
                $("body").removeClass(ClassName.open + " " + ClassName.collapsed);
            }
        }
    };

    // PushMenu Plugin Definition
    // ==========================
    function Plugin(option) {
        return this.each(function () {
            var $this = $(this);
            var data = $this.data(DataKey);

            if (!data) {
                var options = $.extend({}, Default, $this.data(), typeof option === "object" && option);
                $this.data(DataKey, (data = new PushMenu(options)));
            }

            if (option === "toggle") {
                data.toggle();
            }
        });
    }

    var old = $.fn.pushMenu;

    $.fn.pushMenu = Plugin;
    $.fn.pushMenu.Constructor = PushMenu;

    // No Conflict Mode
    // ================
    $.fn.pushMenu.noConflict = function () {
        $.fn.pushMenu = old;
        return this;
    };

    // Data API
    // ========
    $(document).on("click", Selector.button, function (e) {
        e.preventDefault();
        Plugin.call($(this), "toggle");
    });
    $(window).on("load", function () {
        Plugin.call($(Selector.button));
    });
}(jQuery);

+function ($) {
    "use strict";

    var DataKey = "lte.tree";
    var Default = {
        animationSpeed: 10,
        accordion: true,
        followLink: true,
        trigger: ".treeview a"
    };
    var Selector = {
        tree: ".tree",
        treeview: ".treeview",
        treeviewMenu: ".treeview-menu",
        open: ".menu-open, .active",
        li: "li",
        data: "[data-widget='tree']",
        active: ".active"
    };
    var ClassName = {
        open: "menu-open",
        tree: "tree",
        openActive: "menu-open active"
    };
    var Event = {
        collapsed: "collapsed.tree",
        expanded: "expanded.tree"
    };

    // Tree Class Definition
    // =====================
    var Tree = function (element, options) {
        this.element = element;
        this.options = options;
        //$(this.element).addClass(ClassName.tree);
        //$(Selector.treeview + Selector.active, this.element).addClass(ClassName.open);
        $(Selector.treeviewMenu + " " + Selector.active).parents(Selector.treeview).addClass(ClassName.openActive);
        this._setUpListeners();
    };

    Tree.prototype.toggle = function (link, event) {
        var treeviewMenu = link.next(Selector.treeviewMenu);
        var parentLi = link.parent();
        if (!parentLi.is(Selector.treeview)) {
            return;
        }

        if (!this.options.followLink || link.attr("href") === "#" || link.attr("href").indexOf("javascript:") === 0) {
            event.preventDefault();
        }

        if ($(".treeview-menu", parentLi).length > 0) {
            parentLi.removeClass("expandsub");
            if (parentLi.hasClass(ClassName.open)) {
                this.collapse(treeviewMenu, parentLi);
            } else {
                this.expand(treeviewMenu, parentLi);
            }
        }
    };

    Tree.prototype.expand = function (tree, parent) {
        var expandedEvent = $.Event(Event.expanded);
        if (this.options.accordion) {
            var openMenuLi = parent.siblings(Selector.open);
            var openTree = openMenuLi.children(Selector.treeviewMenu);
            this.collapse(openTree, openMenuLi);
        }

        parent.addClass(ClassName.open);
        tree.slideDown(this.options.animationSpeed, function () {
            $(this.element).trigger(expandedEvent);
        }.bind(this));
    };

    Tree.prototype.collapse = function (tree, parentLi) {
        var collapsedEvent = $.Event(Event.collapsed);

        //tree.find(Selector.open).removeClass(ClassName.openActive);
        parentLi.removeClass(ClassName.open);
        tree.slideUp(this.options.animationSpeed, function () {
            tree.find(Selector.open + " > " + Selector.treeview).slideUp();
            $(this.element).trigger(collapsedEvent);
        }.bind(this));
    };

    // Private
    Tree.prototype._setUpListeners = function () {
        var that = this;
        $(this.element).on("click", this.options.trigger, function (event) {
            that.toggle($(this), event);
            if ($(this).next(Selector.treeviewMenu).length === 0 && $(this).attr("href").indexOf("javascript:") < 0) {
                $("li.active", that.element).removeClass("active");
                $(this).parents("li").addClass("active");
            }
        });
    };

    // Plugin Definition
    // =================
    function Plugin(option) {
        return this.each(function () {
            var $this = $(this);
            var data = $this.data(DataKey);
            if (!data) {
                var options = $.extend({}, Default, $this.data(), typeof option === "object" && option);
                $this.data(DataKey, new Tree($this, options));
            }
        });
    }

    var old = $.fn.menuTree;
    $.fn.menuTree = Plugin;
    $.fn.menuTree.Constructor = Tree;

    // No Conflict Mode
    // ================
    $.fn.menuTree.noConflict = function () {
        $.fn.menuTree = old;
        return this;
    };

    // Tree Data API
    // =============
    $(window).on("load", function () {
        $(Selector.data).each(function () {
            Plugin.call($(this));
        });
    });
}(jQuery);

$(document).ready(function () {
    // 左侧菜单点击菜单事件
    tw.bindFill("ul.sidebar-menu a", ".main-body", false, true);
    // 集中管理、默认实例等 Tab 标签切换事件绑定
    bindTabEvent();
    // 设置或重新设置（如事件绑定、赋初始值等）
    setOrReset();
    // 菜单展示优化
    $(".sidebar-menu>li").hover(function () {
        if ($(document.body).hasClass("sidebar-collapse")) {
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

    // 响应式小屏模式下，点击完菜单，自动隐藏左侧菜单栏
    $(".sidebar li a").click(function(){
        if ($(document.body).hasClass("sidebar-open") && $(this).attr("href").indexOf("/") > -1) {
            $(document.body).toggleClass("sidebar-open");
        }
    });
    $(document.body).click(function(e){
        if ($(document.body).hasClass("sidebar-open") && !$(e.target).hasClass(".sidebar-toggle") && $(e.target).parents(".sidebar-toggle").length === 0 && !$(e.target).hasClass(".main-sidebar") && $(e.target).parents(".main-sidebar").length === 0) {
            $(document.body).toggleClass("sidebar-open");
        }
    });

    // ITAIT-4984 微软自研浏览器 Edge 样式特殊处理，解决滚动条样式问题
    var browserInfo = browserNV();
    if (browserInfo != {} && browserInfo.core === "Edge" && browserInfo.v <= 60.0) {
        $(".main-body").css({"min-height": "calc(-100px + 100%)", "height": "auto", "top": "100px", "bottom": "100px"});
    }
    //切换主题模式点击事件
    $("#switch-mode-btn").click(function() {
        var icon = $("i", this);
        var $this = this;
        var themeUrl = $(this).attr("themeUrl");
        var nowTheme = $(this).attr("theme");
        var toTheme = $(this).attr("theme") == "" ? "dark" : "";
        if (themeUrl.indexOf("?") > 0) {
            themeUrl = themeUrl.substr(0, themeUrl.lastIndexOf("/") + 1) + toTheme + themeUrl.substring(themeUrl.indexOf("?"));
        } else {
            themeUrl = themeUrl.substr(0, themeUrl.lastIndexOf("/") + 1) + toTheme;
        }
        $.post(themeUrl, {}, function (themeTxt) {
            $("body").removeClass(nowTheme + "-mode");
            if (themeTxt != "") {
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
        var lis = $(".sidebar-menu .active", getRestrictedArea());
        lis.removeClass("menu-open active");
        for(var i = 0; i < lis.length; i++){
            var uls = lis[i].querySelectorAll('ul');
            for (var j = 0; j < uls.length; j++) {
                uls[j].style.display = 'none';
            }
        }
        $(".sidebar-menu .expandsub", getRestrictedArea()).removeClass("menu-open expandsub");
        var matchPart = $(this).attr("href");
        var menuItemLink = $("ul.sidebar-menu li a[href*='" + matchPart + "']", getRestrictedArea());
        if (menuItemLink.length > 0) {
            $(menuItemLink).parents("li.treeview").addClass("menu-open active");
            $(menuItemLink).parents("ul.treeview-menu").show();
            $(menuItemLink).parent().addClass("active");
            $(menuItemLink).parents(".sidebar.sidebar-scroll").animate({scrollTop: $(menuItemLink).parents(".sidebar.sidebar-scroll").scrollTop() + $(menuItemLink).parent().offset().top - $(menuItemLink).parents(".sidebar.sidebar-scroll").offset().top}, 1000);
        }
        tw.fill(matchPart, {}, ".main-body", false, this);
        return false;
    });
});
/**
 * 设置(如绑定事件或设置初始值等) 或 重新设置
 */
function setOrReset() {
    // 布尔开关
    $(".switch-btn:not(.disallowed)").unbind("click").bind("click", function () {
        $(".switchedge", this).toggleClass("switch-bg");
        $(".circle", this).toggleClass("switch-right");
        $("input", this).val($("input", this).val() === "true" ? false : true);
        $("input", this).change();
    });
    // 下拉列表 / 可输入下拉列表
    niceSelect();
    // 多选下拉
    $("select[multiple='multiple'][loaded!='true']").each(function () {
        var $this = $(this);
        $(this).attr("loaded", true).multipleSelect({
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
    // sortable.jsp 拖拽排序
    dragable();
    // sortablecheckbox.jsp 拖拽排序
    checkboxSortable();
    // 列表页面事件操作
    bindEventForListPage();
    // 重绘搜索框
    resizeFilterForm();
    // form 表单页面事件操作
    bindEventForFormPage();
    // info.jsp 页面加载
    initInfoPage();
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
    $(".markedviewText").each(function() {
        $(this).prev(".markedview").html(marked.parse($(this).val()));
    });
    // 菜单区域禁止鼠标右键
    $(".main-sidebar").unbind("contextmenu").bind("contextmenu", function (e) {
        e.preventDefault();
        return false;
    });
    $("#searchText").unbind("input").bind("input", function (e) {
        var html = "";
        var keyword = $.trim($(this).val());
        if (keyword !== "") {
            $.post(searchUrl, {"q": keyword}, function (data) {
                if (data.success === "true" && data.msg instanceof Array) {
                    var arr = data.msg;
                    var keywordLower = keyword.toLowerCase();
                    for (var i = 0; i < arr.length; i++) {
                        var model = arr[i].model, modelAction = arr[i].modelAction,
                            modelDefAction = arr[i].modelDefAction,
                            modelFieldGroup = arr[i].modelFieldGroup ? arr[i].modelFieldGroup : "OTHERS",
                            modelField = arr[i].modelField, modelFieldName = arr[i].modelFieldName,
                            modelName = arr[i].modelName;

                        var itemHtml = "";
                        var highlight = function (temp) {
                            while (temp.toLowerCase().indexOf(keywordLower) >= 0 || temp.length > 0) {
                                if (temp.toLowerCase().indexOf(keywordLower) >= 0) {
                                    itemHtml += temp.substring(0, temp.toLowerCase().indexOf(keywordLower));
                                    itemHtml += "<span class=\"highlight\">" + temp.substr(temp.toLowerCase().indexOf(keywordLower), keyword.length) + "</span>";
                                    temp = temp.substr(temp.toLowerCase().indexOf(keywordLower) + keyword.length);
                                } else {
                                    itemHtml += temp;
                                    return;
                                }
                            }
                        };

                        if (modelName.toLowerCase().indexOf(keywordLower) >= 0) {
                            itemHtml += "<div class=\"search-item\" onclick=\"gotoTarget('" + model + "', '" + modelDefAction + "', '" + modelFieldGroup + "', '');\">";
                            itemHtml += "<a href=\"javascript:void(0);\">";
                            itemHtml += "<i class=\"icon icon-" + arr[i].modelIcon + "\"></i> ";
                            highlight(modelName);
                            itemHtml += "</a></div>";
                        }

                        if (modelFieldName && modelFieldName !== null && modelFieldName !== "") {
                            itemHtml += "<div class=\"search-item\" onclick=\"gotoTarget('" + model + "', '" + modelAction + "', '" + modelFieldGroup + "', '" + modelField + "');\">";
                            itemHtml += "<a href=\"javascript:void(0);\">";
                            itemHtml += "<i class=\"icon icon-" + arr[i].modelIcon + "\"></i> ";
                            highlight(modelName);

                            if (modelFieldName.toLowerCase().indexOf(keywordLower) >= 0) {
                                itemHtml += " -> ";
                            }
                            highlight(modelFieldName);
                            itemHtml += "</a></div>";
                        }
                        html += itemHtml;
                    }
                }
                $("#searchResult").html(html);
            }, "json");
        } else {
            $("#searchResult").html(html);
        }
    }).unbind("keypress").bind("keypress", function (e) {
        if (e.keyCode === 13) {
            if ($(".search-list .search-item.active").length > 0) {
                $(".search-list .search-item.active").first().click();
            } else {
                $(".search-list .search-item").first().click();
            }
            $("#searchText").blur();
        }
    }).unbind("keydown").bind("keydown", function (e) {
        var active = $(".search-list .search-item.active");
        if (e.keyCode === 40) { // Down
            if ($(active).length > 0) {
                if ($(active).next().length > 0) {
                    $(active).next().addClass("active");
                    $(active).removeClass("active");
                }
            } else {
                $(".search-list .search-item").first().addClass("active");
            }
            return false;
        } else if (e.keyCode === 38) { // Up
            if ($(active).length > 0) {
                if ($(active).prev().length > 0) {
                    $(active).prev().addClass("active");
                    $(active).removeClass("active");
                }
            } else {
                $(".search-list .search-item").first().addClass("active");
            }
            return false;
        } else if (e.keyCode === 27) { // Esc
            $("#searchText").blur();
            return false;
        }
    }).unbind("focus").bind("focus", function (e) {
        var brwVer = browserNV();
        if (brwVer.core === "IE" && brwVer.v === 9) {
            $("#searchResult").css({"visibility": "visible", "opacity": "1"});
        }
    }).unbind("blur").bind("blur", function (e) {
        var brwVer = browserNV();
        if (brwVer.core === "IE" && brwVer.v === 9) {
            $("#searchResult").css({"visibility": "hidden", "opacity": "0"});
        }
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
};

function gotoTarget(model, action, group, field) {
    var boxes = new Array(".content-box>ul>li.active:first", ".content-box>ul>li:eq(1)", ".content-box>ul>li:eq(0)");
    for (var i = 0; i < boxes.length; i++) {
        var menuItemLink = $("ul.sidebar-menu li a[modelName='" + model + "']", $(boxes[i]));
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
                    var container = $("div.bodyDiv", getRestrictedArea());
                    tw.fill("/console/rest/html/" + model + "/" + action + searchUrl.substring(searchUrl.indexOf("?")), {}, ($(container).length > 0 ? container : $(".main-body", getRestrictedArea())), false, menuItemLink);
                }
            }
            $(".nav-tabs a[tabGroup='" + group + "']", getRestrictedArea()).click();
            var count = 12;
            var interval = window.setInterval(function() {
                if (count > 0) {
                    var targetEle = $("label[for='" + field + "']", getRestrictedArea());
                    if ($(targetEle).length > 0) {
                        window.clearInterval(interval);
                        if ($(targetEle).is(":visible")) {
                            var scrollEle = $(".main-body", getRestrictedArea());
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
    var timer = setInterval(function() {
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
    setTimeout(function() {
        clearInterval(timer);
        el.style.left = "0 px";
        el.style.position = "";
    }, maxDistance * interval * quarterCycle);
}
/**************************************** form.jsp - start *************************************************/
function bindEventForFormPage() {
    // 返回按钮
    tw.bindFill(".form-btn a[btn-type='goback']", ".main-body", false, true);

    // 列表页及form页面下载(日志、快照等)
    tw.bindFill(".form-btn a[btn-type='monitor']", ".main-body", true, false);
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
                + "<label id='verifyCodeOtpError' class=\"tw-error-info\" style=\"position:relative; margin-left:-68px; color:red;\"></label>"
                + "</div>";
        openLayer({area: ["450px", "400px"], shadeClose: true, title: getSetting("layerTitleOtp"), content: html, yes: function (index) {
                var params = {};
                params[getSetting("checkOtp")] = $.trim($("#randCode-OTP").val());
                $.ajax({
                    url: (imgSrc.substring(0, imgSrc.lastIndexOf("/"))  + "/confirmKey").replace("/image/", "/json/"),
                    async: true,
                    data: params,
                    dataType: "json",
                    success: function (data) {
                        if (data.success === "true" || data.success === true) {
                            closeLayer(index);
                            showSuccess(getSetting("bindSuccessOtp"));
                        } else {
                            $("#verifyCodeOtpError").html(getSetting("bindFailOtp"));
                        }
                    },
                    error: function (e) {
                        handleError(e);
                    }
                });
            }});
        return false;
    });

    // 只读元素鼠标手势
    $(".form-group [readonly]").each(function () {
        $(this).css("cursor", "not-allowed").parent().css("cursor", "not-allowed");
    });

// 移除：解决多个textare并排显示的样式问题
//    $("textarea[readonly]").each(function (i, elem) {
//        $(this).css("height", elem.scrollHeight);
//    });
    bindFormEvent();

    // 日期组件设置
    $(".form-datetime").datetimepicker({
        weekStart: 1,
        todayBtn: 1,
        autoclose: 1,
        todayHighlight: 1,
        forceParse: 0,
        showMeridian: 1
    });
    (function (json) {
        effectiveInfoFields(eval("(" + (!json || json === '' ? "{}" : json) + ")"));
    })($.trim($("textarea[name='eventConditionsInfoPage']", getRestrictedArea()).val()));
};
function bindFormEvent() {
    $("form[name='pageForm'][loaded!='true']").each(function () {
        var thisForm = $(this);
        // 表单元素级联控制显示隐藏的事件绑定
        bindEvent(eval("(" + $.trim($("textarea[name='eventConditions']", thisForm).val()) + ")"));
        var passwordFields = $.trim($("textarea[name='passwordFields']", thisForm).val()).split(",");
        // form 表单异步提交(ajax form)
        $(this).attr("loaded", "true").ajaxForm({
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
                $("input[data-type='password']").attr("type", "password");
                $("input[data-type='password']").next().find("i").removeClass("icon-eye-open").addClass("icon-eye-close");
                $(".form-btn .btn", thisForm).attr("disabled", true);
                if ($("input[type='file']", thisForm).length > 0) {
                    $(thisForm).attr("enctype", "multipart/form-data");
                } else {
                    $(thisForm).removeAttr("enctype", "multipart/form-data");
                }
                $(".tab-has-error", thisForm).removeClass("tab-has-error");
                $(".form-group .tw-error-info", thisForm).html("");
                $("select[multiple='multiple']", thisForm).multipleSelect("refresh");
                return true;
            },
            success: function (data) {
                $("#mask-loading").hide();
                $(".form-btn .btn", thisForm).removeAttr("disabled");
                if (data.success === "true" || data.success === true) {
                    if ($(".form-btn a[btn-type='goback']", thisForm).length > 0) {
                        $(".form-btn a[btn-type='goback']", thisForm).click();
                    } else {
                        $(".sidebar-menu li.active", getRestrictedArea()).each(function () {
                            if (!$(this).hasClass("menu-open")) {
                                $("a", this).click();
                            }
                        });
                    }

                    if (data.redirectURL !== "" && data.redirectURL !== undefined) {
                        window.location.href = data.redirectURL;
                        return;
                    } else {
                        showSuccess(data.message);
                    }
                } else {
                    $("#tempZone", thisForm).html("");
                    for (var i = 0; i < passwordFields.length; i++) {
                        $("input[name='" + passwordFields[i] + "']", thisForm).val($("input[name='" + passwordFields[i] + "']", thisForm).attr("originVal"));
                    }
                    var first = true;
                    if (data.data && !$.isEmptyObject(data.data)) {
                        var errorData = data.data[0];
                        for (var key in errorData) {
                            $("#form-item-" + key + " > div", thisForm).attr("error-key", key).addClass("has-error");
                            if ($(".nav.nav-tabs", thisForm).length < 1) {
                                $("#form-item-" + key + " > div .tw-error-info", thisForm).html(errorData[key]);
                                if (first) {
                                    first = false;
                                    //$("html, body").animate({scrollTop: $("#form-item-" + key, thisForm).offset().top - 100}, 500);
                                }
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
                                $("label.tw-error-info", this).html(errorData[$(this).attr("error-key")]);
                            });
                        });
                        $($("a", $(".nav.nav-tabs > li.tab-has-error").first()).attr("href")).addClass("active");
                        //$("html, body").animate({scrollTop: $(".has-error", thisForm).first().offset().top - 100}, 500);
                    } else {
                        showError(data.message);
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

/**************************************** form.jsp - end *************************************************/

/**************************************** sortable.jsp - start *************************************************/
function addRow(alink, readonly) {
    if (readonly) {
        return;
    }
    var ulEle = $("ul.sortable", $(alink).parents(".form-control-sortable"));
    var selectedRow = $("li[selected='selected']", ulEle);
    var isIE9 = browserNV().core === "IE" && browserNV().v < 10.0;
    var html = "<li class=\"droptarget\"" + (isIE9 ? "" : " draggable=\"true\"") + "><table class=\"table table-bordered dragtable\">"
        + "<tr><td class=\"editable\" style=\"padding:0px 0px !important;\"><label></label></td>"
        + "<td class=\"narrow\"><a href=\"javascript:void(0);\" class=\"editable-edit\""
        + "onclick=\"bindEditable(this, false);\"><i class=\"icon icon-edit\"></i></a></td>"
        + "<td class=\"narrow\"><a href=\"javascript:void(0);\" onclick=\"removeRow(this, false);\"><i class=\"icon icon-trash\"></i></a></td>"
        + "<td class=\"draggable narrow\"><a href=\"javascript:void(0);\"" + (isIE9 ? " draggable=\"true\"" : "") + "><i class=\"icon icon-arrows\"></i></a>\</td>"
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
                value += tdVal + getSetting("separa");
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
    var isIE9 = browserNV().core === "IE" && browserNV().v < 10.0;
    $(".form-control-sortable>ul.sortable").each(function () {
        if (isIE9) {
            $("li.droptarget", this).removeAttr("draggable");
        } else {
            $("a[draggable='true']", this).removeAttr("draggable");
        }
    });
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
function addDictRow(alink, readonly) {
    if (!readonly) {
        var tr = $(alink).parent().parent();
        var html = "<tr>"
            + "<td class=\"edit-kv\" style=\"padding:0px 0px !important;\"><input type=\"text\" class=\"form-control\" value='' onchange=\"refreshDict()\" /></td>"
            + "<td class=\"edit-kv\" style=\"padding:0px 0px !important;\"><input type=\"text\" class=\"form-control\" value='' onchange=\"refreshDict()\" /></td>"
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
                value += entry + getSetting("separa");
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
        $("input", this).attr("checked", !$("input:not([readonly]", this).attr("checked"));
        if ($(this).parent().is(".sortable")) {
            $("label", this).css({"cursor": ($("input", this).is(":checked")) ? "move" : "default"});
        }
    });
    var draging = null;
    $(".checkbox-group a input:not(:checked):first", getRestrictedArea()).closest("a").before($(".checkbox-group a input:checked", getRestrictedArea()).closest("a"));
    $(".checkbox-group a input:checked:not([readonly])").next().css({"cursor": "move"});
    $(".checkbox-group.sortable").unbind("selectstart,dragstart,drag,dragend,dragenter,dragover,dragleave,drop")
        .bind("selectstart", function (e) {
            e.preventDefault();
            return false;
        }).bind("dragstart", function (e) {
        draging = e.target;
    }).bind("dragover", function (e) {
        e.preventDefault();
        var target = e.target.nodeName === "A" ? e.target : $(e.target).parents("a[draggable='true']")[0];
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
    $("ul.pager.pager-loose").each(function () {
        var partLinkUri = $(this).attr("partLinkUri");
        $(this).pager({
            page: Number($(this).attr("data-page")),
            recPerPage: Number($(this).attr("recPerPage")),
            recTotal: Number($(this).attr("data-rec-total")),
            linkCreator: function (page, pager) {
                var uri = partLinkUri + page;// + '&' + $("#filterForm").serialize()
                return encodeURI(uri);
            },
            lang: getSetting("pageLang")
        });
    });
    // 搜索
    tw.bindFill(".search-btn a", ".main-body", false, false);
    // 列表搜索框回车
    $("form[name='filterForm']").unbind("keypress").bind("keypress", function (e) {
        if (e.keyCode === 13) {
            e.preventDefault();
            var $this = $(this);
            var filterForm = document.getElementById("filterForm");
            var element = filterForm.getElementsByTagName("a")[0];
            var url = element.href;
            var data = tw.formToJson($("form[name='filterForm']", getRestrictedArea()));
            var targetContainer = $this.attr("fill") || $(".main-body", getRestrictedArea());
            tw.fill(url, data, targetContainer, false, this);
        }
    });
    // 表格顶部操作按钮
    tw.bindFill(".tools-group a", ".bodyDiv", false, true);
    // 表格操作列
    tw.bindFill("table a.dataid", ".bodyDiv", false, true);
    // 列表页
    tw.bindFill("a.tw-action-link", ".main-body", true, true);
    // 分页
    tw.bindFill("ul.pager.pager-loose a", ".main-body", false, false);
    // 列表操作列部分操作事件绑定
    $("a[act-ajax='true']").unbind("click").bind("click", function (e) {
        e.preventDefault();
        var actUrl = $(this).attr("href");
        showConfirm($(this).attr("act-confirm"), {
            "title": getSetting("pageConfirmTitle"),
            "btn": [getSetting("confirmBtnText"), getSetting("cancelBtnText")]
        }, function (index) {
            closeLayer(index);
            confirm_method(actUrl);
        });
        return false;
    });
    // 集群实例点击[管理]，打开新 Tab 并切换
    $("table a[action-name='" + getSetting("actionName_target") + "']")
    .unbind("click").bind("click", function (e) {
        e.preventDefault();
        var tab = $(".tab-box>ul>li[bind-id='" + $(this).attr("data-id") + "']");
        if (tab.length > 0) {
            tab.click();
            return;
        }
        var url = $(this).attr("href");
        var tabHtml = "<li id=\"" + new Date().getTime() + "\" bind-id=\"" + $(this).attr("data-id") + "\">"
            + "<a href=\"javascript:void(0);\" href-attr=\"" + url + "\" rel=\"noopener noreferrer\">"
            + "    <i class=\"icon icon-" + $(this).attr("model-icon") + "\"></i>"
            + "    <label>" + $(this).attr("data-name") + "</label>"
            + "    <span class=\"noticeNumber label label-badge\" style=\"display:none;\">0</span>"
            + "</a>"
            + "<label class=\"close\">"
            + "    <i class=\"icon icon-times\"></i>"
            + "</label>"
            + "</li>";
        $(".tab-box>ul").append(tabHtml);
        $(".content-box>ul").append("<li></li>");
        bindTabEvent();
        $(".tab-box>ul>li").last().click();
        tw.fill(url, {}, $(".content-box>ul>li").last(), false, this);
        tw.bindFill("ul.sidebar-menu a", ".main-body", false, true);
        $("ul[data-widget='tree']", $(".content-box>ul>li").last()).menuTree();
        $("[data-toggle='push-menu']", $(".content-box>ul>li").last()).pushMenu({});
        return false;
    });

    // 列表页及form页面下载(日志、快照等)
    $("table a[action-name='" + getSetting("filesName") + "'], a[btn-type='" + getSetting("filesName") + "']").unbind("click").bind("click", function (e) {
        e.preventDefault();
        if($(this).attr("href") !== "#" && $(this).attr("href").indexOf("javascript:") < 0){
            downloadFiles($(this).attr("href"), $(this).attr("downloadfile"));
        }
        return false;
    });

    // 分页
    tw.bindFill("table a[record-action-id='" + getSetting("showAction") + "']", ".main-body", true, true);
};

/**
 * @param url
 */
function confirm_method(url) {
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
                $(".search-btn a", getRestrictedArea()).click();
                showSuccess(data.message);
            } else {
                showError(data.message);
            }
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
            if (data.data && !$.isEmptyObject(data.data)) {
                var keys = [];
                var groups = {};
                var defGroup = "defaultGroup-" + randId;
                var attachmentData = data.data[0];
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
            html += "<lable id='fileErrorMsg-" + randId + "' style='height: 20px; color: red; "
                + (data.success === "true" ? "display: none;'>" : ("display: block;'>" + data.msg)) + "</lable>";
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
                                partUrl += getSetting("separa");
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
};
/**************************************** list.jsp - end *************************************************/

/**************************************** info.jsp - start *************************************************/
function initInfoPage() {
    var randId = new Date().getTime();
    $(".bodyDiv>div.infoPage[chartMonitor='true'][loaded!='true']").each(function (i) {
        $(this).attr("loaded", "true");
        var thisDiv = $(this);
        var chartOption = defaultOption();
        var infoKeys = eval("(" + $("textarea[name='infoKeys']", thisDiv).val() + ")");
        var myChart = echarts.init($("div.block-bg[container='chart']", this)[0]);
        (function (chartObj, option, url, keys, restrictedArea, tempId) {
            $(thisDiv).append("<span id=\"monitor-timer-" + tempId + "\" style=\"display:none;\"></span>");
            var retryOption = {retryLimit: 10};
            window.setTimeout(function fn(retryRemain) {
                if (retryRemain === undefined) {
                    retryRemain = retryOption.retryLimit;
                }
                if (retryRemain > 0 && retryRemain <= retryOption.retryLimit && $("span#monitor-timer-" + tempId).length > 0) {
                    retryOption["retryRemain"] = retryRemain;
                    var bodyWidth = $(document.body).width();
                    if (bodyWidth <= 1200) {
                        option.legend.textStyle.fontSize = 11;
                        option.grid.top = "15%";
                    } else if (bodyWidth < 1600 && bodyWidth > 1200) {
                        option.legend.textStyle.fontSize = 12;
                        option.grid.top = "12%";
                    } else {
                        option.legend.textStyle.fontSize = 13;
                        option.grid.top = "10%";
                    }
                    handler(chartObj, option, url, keys, restrictedArea, retryOption, fn);
                }
            }, 10);
        })(myChart, chartOption, $(thisDiv).attr("data-url"), infoKeys, thisDiv, randId + i);
    });
};

// 获取当前时间的分钟秒
function getTime() {
    var myDate = new Date();
    var hours = myDate.getHours < 10 ? "0" + myDate.getHours() : myDate.getHours();
    var minutes = myDate.getMinutes() < 10 ? "0" + myDate.getMinutes() : myDate.getMinutes();
    var seconds = myDate.getSeconds() < 10 ? "0" + myDate.getSeconds() : myDate.getSeconds();
    return hours + ":" + minutes + ":" + seconds;
};

function defaultOption() {
    return {
        width: 'auto',
        title: {text: ''},
        tooltip: {trigger: 'axis'},
        legend: {data: [], textStyle: {fontSize: 13,color: "#9e9e9e"}, align: 'auto'},
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
            window.setTimeout(function () {
                timerFn(retryOption.retryRemain);
            }, 2000);
        },
        success: function (data) {
            if (data.success === "true" || data.success === true) {
                var monitorData = data.data[0];
                if (monitorData !== null && JSON.stringify(monitorData) !== '{}') {
                    var models = [{
                        dataTime: getTime(),
                        data: monitorData,
                        models: data.data[1]
                    }];
                    addData(chartObj, chartOption, models, keys, restrictedArea);
                }
            } else {
                showError(data.message);
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
        var legend = keys[key];
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
        option.xAxis.data.push(models[i].dataTime);
        var data = models[i].data;
        for (var key in keys) {
            var value = data[key];
            if (!(/(^[0-9]\d*$)/.test(value))) {
                valueIsItAnInteger = 'false';
            }
            for (var seriesKey in option.series) {
                if (option.series[seriesKey].name === keys[key]) {
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
    // 更新数据展示
    chartObj.setOption(option, false);
    chartObj.resize();
    panelUpdate(models, restrictedArea);
};
function panelUpdate(datas, restrictedArea) {
    if (datas.length > 0) {
        var data = datas[0];
        $(".panel-body table tr", restrictedArea).each(function () {
            $($("td", this)[1]).text(data[$($("td", this)[0]).attr("field")]);
        });
    }
    if (datas.length == 0) {
        return;
    }
    $(".panel-body table tr", restrictedArea).each(function () {
        var key = $($("td", this)[0]).attr("key");
        var fieldName = $($("td", this)[0]).attr("field");
        var text = undefined;

        for (var i in datas) {
            for (var k in datas[i]) {
                if (k != "dataTime") {
                    if (datas[i][k] instanceof Array) {
                        if (datas[i][k].length > 0) {
                            text = datas[i][k][0][fieldName];
                        }
                    } else if (datas[i][k] instanceof Object) {
                        if (key !== undefined) {
                            text = datas[i][k][key][fieldName];
                        } else {
                            text = datas[i][k][fieldName];
                        }
                    } else if (datas[i][k] instanceof String && k == fieldName) {
                        text = datas[i][k];
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
/**************************************** info.jsp - end *************************************************/
/**
 * 绘制 / 结束绘制引导锚点
 * @param {boolean} off 关闭绘制锚点 true | false
 * @param {String} image 背景图片名称，带扩展名，图片位于 WEB-INF 同级目录的 static 下的 images/guide目录下
 */
function markAnchor(off, image) {
    var stepsArray = [];
    var stepOption = {};
    var guideBoard = document.getElementById("guide");
    if (off) {
        guideBoard.onmousedown = null;
        $(document.body).children("header,main").show();
        $(guideBoard).hide();
        return;
    }
    var imgSrc = $("#guideImg", guideBoard).attr("src");
    if (image !== undefined && image !== "") {
        $("#guideImg", guideBoard).attr("src", imgSrc.substring(0, imgSrc.lastIndexOf("/") + 1) + image);
    } else {
        image = imgSrc.substring(imgSrc.lastIndexOf("/") + 1);
    }
    $(document.body).children("header,main").hide();
    $(guideBoard).show();
    guideBoard.onmousedown = function (e) {
        stepOption = {};
        var offset = 0;
        e = e || window.event;
        var x1 = e.clientX + offset, y1 = e.clientY + offset;

        var marker = document.createElement("div");
        marker.id = randomId(6);
        marker.className = "guideMarker";
        guideBoard.appendChild(marker);

        stepOption["element"] = "#" + marker.id;
        stepOption["intro"] = "第" + (stepsArray.length + 1) + "步引导信息";
        stepOption["image"] = image;
        guideBoard.onmousemove = function (ev) {
            ev = ev || window.event;
            var x2 = ev.clientX + offset, y2 = ev.clientY + offset;
            var left = x2 > x1 ? x1 : x2;
            var top = y2 > y1 ? y1 : y2;
            stepOption["position"] = "auto";
            stepOption["rl"] = left * 100 / guideBoard.clientWidth;
            stepOption["rt"] = top * 100 / guideBoard.clientHeight;
            stepOption["rw"] = Math.abs(x2 - x1) * 100 / guideBoard.clientWidth;
            stepOption["rh"] = Math.abs(y2 - y1) * 100 / guideBoard.clientHeight;

            marker.style.left = left + "px";
            marker.style.top = top + "px";
            marker.style.width = Math.abs(x2 - x1) + "px";
            marker.style.height = Math.abs(y2 - y1) + "px";
            marker.style.border = "2px dotted red";
        };

        document.onmouseup = function () {
            if (stepOption !== null) {
                stepsArray.push(stepOption);
                // 清空事件绑定
                guideBoard.onmousemove = null;
                /*$("div.guideMarker", guideBoard).each(function () {
                    $(this).css({"border": "0px"});
                });*/
                //alert("F12 打开开发者工具，查看控制台输出并复制“需要拷贝的参数”。");
                console.log("需要拷贝的参数，对应 guideOptions.steps：" + JSON.stringify(stepsArray));
                stepOption = null;
            }
        };

        return false;// 解除在划动过程中鼠标样式改变的BUG
    };
}
/**
 * 生成随机id
 * @param {type} length
 * @returns {String}
 */
function randomId(length) {
    length = length ? length : 6;
    var random = "";
    var chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    for (var i = length; i > 0; --i) {
        random += chars[Math.floor(Math.random() * chars.length)];
    }
    return random;
}

function base64Encode(input) {
    keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    var output = "";
    var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
    var i = 0;
    input = utf8_encode(input);
    while (i < input.length) {
        chr1 = input.charCodeAt(i++);
        chr2 = input.charCodeAt(i++);
        chr3 = input.charCodeAt(i++);
        enc1 = chr1 >> 2;
        enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
        enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
        enc4 = chr3 & 63;
        if (isNaN(chr2)) {
            enc3 = enc4 = 64;
        } else if (isNaN(chr3)) {
            enc4 = 64;
        }
        output = output +
            keyStr.charAt(enc1) + keyStr.charAt(enc2) +
            keyStr.charAt(enc3) + keyStr.charAt(enc4);
    }
    return output;
}

function utf8_encode(string) {
    string = string.replace(/\r\n/g, "\n");
    var utftext = "";
    for (var n = 0; n < string.length; n++) {
        var c = string.charCodeAt(n);
        if (c < 128) {
            utftext += String.fromCharCode(c);
        } else if ((c > 127) && (c < 2048)) {
            utftext += String.fromCharCode((c >> 6) | 192);
            utftext += String.fromCharCode((c & 63) | 128);
        } else {
            utftext += String.fromCharCode((c >> 12) | 224);
            utftext += String.fromCharCode(((c >> 6) & 63) | 128);
            utftext += String.fromCharCode((c & 63) | 128);
        }
    }
    return utftext;
}

function resizeFilterForm() {
    let screenWidth = window.innerWidth;
    let divElements = document.querySelectorAll('.filterForm > div');
    if (divElements.length > 0) {
        for (let i = 0; i < divElements.length; i++) {
            divElements[i].style.width = '';
        }
        if (screenWidth >= 1200 && divElements.length > 9) {
            for (let i = 0; i < divElements.length; i++) {
                divElements[i].style.width = '12.5%';
            }
        } else if (screenWidth >= 1200 && divElements.length <= 9) {
            for (let i = 0; i < divElements.length - 1; i++) {
                divElements[i].style.width = (92 / (divElements.length - 1)) + '%';
            }
            divElements[divElements.length - 1].style.width = '7%';
        } else if (screenWidth >= 992 && screenWidth < 1200 && divElements.length <= 7) {
            for (let i = 0; i < divElements.length - 1; i++) {
                divElements[i].style.width = (86 / (divElements.length - 1)) + '%';
            }
            divElements[divElements.length - 1].style.width = '14%';
        } else if (screenWidth >= 768 && screenWidth < 992 && divElements.length <= 5) {
            for (let i = 0; i < divElements.length; i++) {
                divElements[i].style.width = (80 / (divElements.length - 1)) + '%';
            }
            divElements[divElements.length - 1].style.width = '20%';
        } else if (screenWidth < 768 && divElements.length <= 4) {
            for (let i = 0; i < divElements.length - 1; i++) {
                divElements[i].style.width = (75 / (divElements.length - 1)) + '%';
            }
            divElements[divElements.length - 1].style.width = '25%';
        }
    }
}
