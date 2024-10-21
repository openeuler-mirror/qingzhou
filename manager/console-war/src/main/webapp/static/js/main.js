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
        if (this.options.expandOnHover || ($("body").is(Selector.mini + Selector.layoutFixed))) {
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
            // tree.find(Selector.open + " > " + Selector.treeview).slideDown();
            $(this.element).trigger(expandedEvent);
        }.bind(this));
    };

    Tree.prototype.collapse = function (tree, parentLi) {
        var collapsedEvent = $.Event(Event.collapsed);

        //tree.find(Selector.open).removeClass(ClassName.openActive);
        parentLi.removeClass(ClassName.open);
        tree.slideUp(this.options.animationSpeed, function () {
            // tree.find(Selector.open + " > " + Selector.treeview).slideUp();
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
                $(this).parents("li").not(".treeview.menu-open").addClass("active");
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

var b64pad = "=";
var b64map = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

function hex2b64(h) {
    var i, c;
    var ret = "";
    for (i = 0; i + 3 <= h.length; i += 3) {
        c = parseInt(h.substring(i, i + 3), 16);
        ret += b64map.charAt(c >> 6) + b64map.charAt(c & 63);
    }
    if (i + 1 === h.length) {
        c = parseInt(h.substring(i, i + 1), 16);
        ret += b64map.charAt(c << 2);
    } else if (i + 2 === h.length) {
        c = parseInt(h.substring(i, i + 2), 16);
        ret += b64map.charAt(c >> 2) + b64map.charAt((c & 3) << 4);
    }
    while ((ret.length & 3) > 0) {
        ret += b64pad;
    }
    return ret;
};

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
        output = output + keyStr.charAt(enc1) + keyStr.charAt(enc2) + keyStr.charAt(enc3) + keyStr.charAt(enc4);
    }
    return output;
};

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
};

JSEncrypt.prototype.encryptLong2 = function (string) {
    var k = this.getKey();
    try {
        var ct = "";
        var bytes = new Array();
        bytes.push(0);
        var byteNo = 0;
        var len, c;
        len = string.length;
        var temp = 0;
        for (var i = 0; i < len; i++) {
            c = string.charCodeAt(i);
            if (c >= 0x010000 && c <= 0x10FFFF) {
                byteNo += 4;
            } else if (c >= 0x000800 && c <= 0x00FFFF) {
                byteNo += 3;
            } else if (c >= 0x000080 && c <= 0x0007FF) {
                byteNo += 2;
            } else {
                byteNo += 1;
            }
            if ((byteNo % 117) >= 114 || (byteNo % 117) === 0) {
                if (byteNo - temp >= 114) {
                    bytes.push(i);
                    temp = byteNo;
                }
            }
        }
        if (bytes.length > 1) {
            for (var i = 0; i < bytes.length - 1; i++) {
                var str;
                if (i === 0) {
                    str = string.substring(0, bytes[i + 1] + 1);
                } else {
                    str = string.substring(bytes[i] + 1, bytes[i + 1] + 1);
                }
                var t1 = k.encrypt(str);
                ct += t1;
            }
            if (bytes[bytes.length - 1] !== string.length - 1) {
                var lastStr = string.substring(bytes[bytes.length - 1] + 1);
                ct += k.encrypt(lastStr);
            }
            return hex2b64(ct);
        }
        return hex2b64(k.encrypt(string));
    } catch (ex) {
        return false;
    }
};

/**
 * 统一处理ajax异常
 * @param {*} e
 */
function handleError(e) {
    if (e.status === 0) {
        showMsg(getSetting("networkError"), "error");
    } else if (e.status === 403) {
        showMsg("403, Access denied !", "error");
        $("#mask-loading").hide();
    } else {
        if (e.status === 200 && e.responseText.indexOf("<!DOCTYPE html>") >= 0 && e.responseText.indexOf("loginForm") > 0) {
            $("#mask-loading").hide();
            $(".btn").removeAttr("disabled");
            showConfirm(getSetting("notLogin"), {
                "title": getSetting("pageConfirmTitle"),
                "btn": [getSetting("reloginBtnText"), getSetting("iknowBtnText")]
            }, function () {
                var loginUrl = window.location.href.substring(0, window.location.href.indexOf(window.location.pathname))
                    + "/" + window.location.pathname.replace("/", "").substring(0, window.location.pathname.replace("/", "").indexOf("/"));
                window.location.href = loginUrl;
            }, function () {
            });
        } else {
            showMsg(e.status + ":" + getSetting("pageErrorMsg"), "error");
        }
    }
};

function showMsg(title, type, callback) {
    var container = document.body;
    try {
        container = getRestrictedArea();
    } catch (e) {
    }
    var time = 5000;
    if (type === "info") {
        type = "success";
        time = 1800;
    } else if (type === "warn") {
        type = "warning";
        time = 1800;
    }
    return cocoMessage.resmsg({"msgWrapperContainer": container, msg: title, duration: time, onClose: callback}, type);
};

//showinfo不能去，否则会和showmsg的type重复
function showInfo(title, time) {
    var container = document.body;
    try {
        container = getRestrictedArea();
    } catch (e) {
    }
    return cocoMessage.info({"msgWrapperContainer": container, msg: title, duration: (time ? time : 0)});
};

function shakeTip(msg) {
    layer.msg(msg, function () {
    });
};

function showConfirm(confirmMsg, options, yesFn, noFn) {
    var theme = $("#switch-mode-btn").attr("theme");
    if (theme != "") {
        options["skin"] = "layer-" + theme;
    }
    return layer.confirm(confirmMsg, options, function (index) {
        var caller = {};
        caller.fn = yesFn;
        caller.fn(index);
    }, function (index) {
        if (noFn) {
            var caller = {};
            caller.fn = noFn;
            caller.fn(index);
        }
    });
};

function openLayer(options) {
    var defaults = {
        area: ["600px", "360px"], yes: function () {
            // do nothing
        }
    };
    var theme = $("#switch-mode-btn").attr("theme");
    if (theme != "") {
        options["skin"] = "layer-" + theme;
    }
    return layer.open($.extend(defaults, options));
}

function closeLayer(index) {
    if (document.getElementById(index)) {
        cocoMessage.close(index);
    } else {
        layer.close(index);
    }
}

// start, for: ModelField注解 effectiveWhen()
function bindEvent(showCondition) {
    const triggers = {};
    const contain = function (array, ele) {
        for (var i = 0; i < array.length; i++) {
            if (array[i] === ele) {
                return true;
            }
        }
        return false;
    };

    // 处理 show 条件
    if (showCondition) {
        Object.keys(showCondition).forEach(fieldName => {
            const condition = showCondition[fieldName];
            const expressions = condition.includes("&") ? condition.split("&") : condition.split("|");

            // 处理每个表达式
            expressions.forEach(expression => {
                const operator = expression.includes("!=") ? "!=" : "=";
                const [triggerItem, val] = expression.split(operator).map(part => part.trim());

                // 初始化触发器对象
                if (!triggers[triggerItem]) {
                    triggers[triggerItem] = [];
                }

                // 创建并添加条件
                const json = {[fieldName]: condition};
                if (!contain(triggers[triggerItem], json)) {
                    triggers[triggerItem].push(json);
                }
            });
        });
    }

    var triggerTies = function (json) {
        json.forEach(item => {
            const {type, ...rest} = item;
            Object.keys(rest).forEach(key => {
                triggerAction(key, rest[key]);
            });
        });
    };
    const restrictedArea = getRestrictedArea();
    // 绑定事件和触发初始状态
    Object.keys(triggers).forEach(item => {
        const elements = $(`[name='${item}']`, restrictedArea);
        elements.off("change").on("change", function () {
            triggerTies(triggers[$(this).attr("name")]);
        });
        triggerTies(triggers[item]);
    });

    autoAdaptTip();
}

function triggerAction(ele, condition) {
    const operators = condition.includes("&") ? "&" : "|";
    const expressions = condition.split(operators);
    const compareVal = function (value, val, notEq) {
        return notEq ? value !== val : value === val;
    };
    const restrictedArea = getRestrictedArea();

    let shouldShow;

    expressions.forEach(expression => {
        let compareResult = false;
        if (!expression.includes("!=") && !expression.includes("=")) {
            compareResult = true;
        } else {
            const notEq = expression.includes("!=");
            const operator = notEq ? "!=" : "=";
            const [item, val] = expression.split(operator);
            const parsedVal = val.includes("'") || val.includes("\"") ? eval(val) : val;

            // 优化目标元素查找
            let target = $("[name='" + item + "']:selected", restrictedArea).length > 0 ?
                $("[name='" + item + "']:selected", restrictedArea) : $("[name='" + item + "']:checked", restrictedArea);

            if (!target.length) {
                target = $("[name='" + item + "']", restrictedArea);
            }

            // 当有多个选项未选中时，隐藏目标表单项
            if ((target.length > 1 || !target.length)) {
                $("#form-item-" + ele, restrictedArea).hide();
                return;
            }
            let isSwitch = target.parent().attr("class").indexOf("switch") !== -1;
            if (target.val() === "" && isSwitch) {
                target.val("false");
            }
            // compareVal 比较出来的值是表示是否显示
            compareResult = compareVal(target.val(), parsedVal, notEq);
        }
        if (shouldShow === undefined) {
            shouldShow = compareResult;
        } else {
            if (operators === "&") {
                shouldShow = shouldShow && compareResult;
            } else {
                shouldShow = shouldShow || compareResult;
            }
        }
    });

    if (shouldShow) {
        $("#form-item-" + ele, restrictedArea).fadeIn(200);
    } else {
        $("#form-item-" + ele, restrictedArea).hide();
    }
}

/*下拉列表，可输入下拉列表：jQuery Nice Select - v1.1.0 (Made by Hernán Sartorio, https://github.com/hernansartorio/jquery-nice-select)*/
function niceSelect() {
    /* Event listeners */
    // Unbind existing events in case that the plugin has been initialized before
    $(document).off(".nice_select");
    $(".form-control.nice-select").each(function () {
        var $dropdown = $(this);
        $("input[type='text']", this).unbind("input").bind("input", function () {
            var input = $.trim($(this).val()).toLowerCase();
            $dropdown.attr("title", $(this).val());
            if ($(this).is(":focus")) {
                $("li.option", $dropdown).each(function () {
                    $(this).text().toLowerCase().indexOf(input) < 0 ? $(this).addClass("unmatch") : $(this).removeClass("unmatch");
                });
            }
        }).unbind("blur").bind("blur", function () {
            $(this).val($(this).attr("text"));
        });
        if ($(".option.selected.focus", this).length === 0 && $("li", this).length > 0) {
            $("li:first", this).addClass("selected focus");
            $("input[type='hidden']:not([readonly])", this).val($("li:first", this).attr("data-value"));
            $("input[type='text']", this).val($("li:first", this).text());
            if ($("span:first", this).length > 0) {
                if ($.trim($("li:first", this).text()) !== "") {
                    $(this).attr("title", $("li:first", this).text());
                } else {
                    $(this).attr("title", $("li:first", this).attr("data-value"));
                }
                $("span:first", this).text($(this).attr("title"));
            } else {
                $(this).attr("title", $("li:first", this).attr("data-value"));
                $("input[type='hidden']", this).val($("li:first", this).attr("data-value"));
                $("input[type='hidden']", this).attr("format", $("li:first", this).attr("format"));
            }
        }
    });
    // Open/close
    $(document).on("click.nice_select", ".nice-select", function (e) {
        var $dropdown = $(this);
        $(".nice-select").not($dropdown).removeClass("open");
        $dropdown.toggleClass("open");
        if ($dropdown.hasClass("open")) {
            $(".focus", $dropdown).removeClass("focus");
            $(".selected", $dropdown).addClass("focus");
        } else {
            $dropdown.focus();
        }
        if ((document.body.offsetHeight - $(this).offset().top) < $("ul.list", this).height()) {
            $("ul.list", this).css({"margin-top": (0 - $("ul.list", this).height() - 38) + "px"});
        }
    });

    // Close when clicking outside
    $(document).on("click.nice_select", function (e) {
        if ($(e.target).closest(".nice-select").length === 0) {
            $(".nice-select").removeClass("open");
            $(".nice-select li.option").removeClass("unmatch");
        }
    });

    // Option click
    $(document).on("click.nice_select", ".nice-select .option:not(.disabled)", function (e) {
        var $option = $(this);
        var $dropdown = $option.closest(".nice-select");
        $(".selected", $dropdown).removeClass("selected");
        $option.addClass("selected");

        $("input[type='hidden']", $dropdown).val($option.attr("data-value"));
        $("input[type='hidden']", $dropdown).attr("format", $option.attr("format"));
        $("input[type='text']", $dropdown).attr("text", $option.text()).val($option.text());
        $("input[type='hidden']", $dropdown).change();
        if ($("span", $dropdown).length > 0) {
            if ($.trim($option.text()) !== "") {
                $("span", $dropdown).html($option.text());
            } else {
                $("span", $dropdown).html($option.attr("data-value"));
            }
            $dropdown.attr("title", $("span", $dropdown).html());
        } else {
            $dropdown.attr("title", $option.attr("data-value"));
        }
    });

    // Keyboard events
    $(document).on("keydown.nice_select", ".nice-select", function (event) {
        var $dropdown = $(this);
        var $focused_option = $($(".focus", $dropdown) || $(".list .option.selected", $dropdown));
        // Space or Enter
        if (event.keyCode === 32 || event.keyCode === 13) {
            if ($dropdown.hasClass("open")) {
                $focused_option.trigger("click");
            } else {
                $dropdown.trigger("click");
            }
            return false;
        } else if (event.keyCode === 40) { // Down
            if (!$dropdown.hasClass("open")) {
                $dropdown.trigger("click");
            } else {
                var $next = $focused_option.nextAll(".option:not(.disabled)").first();
                if ($next.length > 0) {
                    $(".focus", $dropdown).removeClass("focus");
                    $next.addClass("focus");
                }
            }
            return false;
        } else if (event.keyCode === 38) { // Up
            if (!$dropdown.hasClass("open")) {
                $dropdown.trigger("click");
            } else {
                var $prev = $focused_option.prevAll(".option:not(.disabled)").first();
                if ($prev.length > 0) {
                    $(".focus", $dropdown).removeClass("focus");
                    $prev.addClass("focus");
                }
            }
            return false;
        } else if (event.keyCode === 27) { // Esc
            if ($dropdown.hasClass("open")) {
                $dropdown.trigger("click");
            }
        } else if (event.keyCode === 9) { // Tab
            if ($dropdown.hasClass("open")) {
                return false;
            }
        }
    });

    // Detect CSS pointer-events support, for IE <= 10. From Modernizr.
    var style = document.createElement("a").style;
    style.cssText = "pointer-events:auto";
    if (style.pointerEvents !== "auto") {
        $("html").addClass("no-csspointerevents");
    }
}

function difModelActive(omodel, nmodel) {
    if (omodel !== nmodel) {
        var omenuItemLink = $("ul.sidebar-menu li a[modelName='" + omodel + "']");
        var nmenuItemLink = $("ul.sidebar-menu li a[modelName='" + nmodel + "']");
        if (omenuItemLink.length > 0) {
            if ($(omenuItemLink).parent().hasClass("treeview")) {
                $(omenuItemLink).parents("li.treeview").removeClass("active");
            } else {
                $(omenuItemLink).parents("li.treeview").removeClass("menu-open active");
            }
            $(omenuItemLink).parents("ul.treeview-menu").hide();
            $(omenuItemLink).parent().removeClass("active");
        }
        if (nmenuItemLink.length > 0) {
            if ($(nmenuItemLink).parent().hasClass("treeview")) {
                $(nmenuItemLink).parents("li.treeview").addClass("active");
            } else {
                $(nmenuItemLink).parents("li.treeview").addClass("menu-open active");
            }
            $(nmenuItemLink).parents("ul.treeview-menu").show();
            $(nmenuItemLink).parent().addClass("active");
        }
    }
}

function batchOps(url, action) {
    var params = "";
    $(".list-table  input[type='checkbox'][class='morecheck']", getRestrictedArea()).each(function () {
        if ($(this).prop("checked")) {
            if ($(this).attr("value") !== undefined && $(this).attr("value") !== null && $(this).attr("value") !== "") {
                params = params + $(this).attr("value") + "<%=idFieldInfo.getSeparator()%>"
            }
        }
    });
    var str = url;
    if (str.indexOf("?") > -1) {
        url = str + "&<%=idField%>=" + params;
    } else {
        url = str + "?<%=idField%>=" + params;
    }
    $("#" + action, getRestrictedArea()).attr("href", url);
}

function filter_reset(subEle) {
    const form = $(subEle).parents('.filterForm')[0];
    form.reset();
    $(".filter_search", form).click();
}
