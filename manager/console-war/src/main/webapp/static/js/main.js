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
// 当前页面主活动区域
function getActiveTabContent() {
    return $(".content-box>ul>li")[$(".tab-box>ul>li.active").index()];
};
// 绑定本机、集群、实例 Tab 标签事件
function bindTabEvent() {
    var now = new Date().getTime();
    $(".tab-box>ul>li").each(function (i) {
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
// 获取限定区域
function getRestrictedArea() {
    return getActiveTabContent();
};
function autoAdaptTip() {
    var mainBody = $(".main-body", getRestrictedArea());
    if (mainBody.length > 0) {
        var ruler = document.createElement("div");
        ruler.style.fontSize = "13px";
        ruler.style.maxWidth = "320px";
        ruler.style.visibility = "hidden";
        document.body.appendChild(ruler);
        var totalHeight = $(mainBody).prop("scrollHeight");
        var scrollTop = $(mainBody).scrollTop();
        var offsetTop = $(mainBody).offset().top;
        $("form[name='pageForm'] span.tooltips:visible", mainBody).each(function() {
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
function showMsg(title,type,callback) {
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
    return cocoMessage.resmsg({"msgWrapperContainer": container, msg: title, duration: time, onClose: callback},type);
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
    layer.msg(msg, function(){});
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
    var defaults = {area: ["600px", "360px"], yes: function () {
        // do nothing
    }};
    var theme = $("#switch-mode-btn").attr("theme");
    if (theme != "") {
        options["skin"] = "layer-" + theme;
    }
    return layer.open($.extend(defaults, options));
};
function closeLayer(index) {
    if (document.getElementById(index)) {
        cocoMessage.close(index);
    } else {
        layer.close(index);
    }
};

// start, for: ModelField注解 effectiveWhen()
function bindEvent(conditions) {
    var triggers = {};
    for (var key in conditions) {
        var condition = conditions[key];
        var array;
        if (condition.indexOf("&") >= 0) {
            array = condition.split("\&");
        } else {
            array = condition.split("\|");
        }

        for (var i = 0; i < array.length; i++) {
            var operator = array[i].search("!=") < 0 ? "=" : "!=";
            var triggerItem = array[i].split(operator)[0];
            if (!triggers[triggerItem]) {
                triggers[triggerItem] = [];
            }
            var json = {};
            json[key] = condition;
            if (!contain(triggers[triggerItem], json)) {
                triggers[triggerItem].push(json);
            }
        }
    }
    for (var item in triggers) {
        $("[name='" + item + "']", getRestrictedArea()).unbind("change").bind("change", function () {
            triggerTies(triggers[$(this).attr("name")]);
        });
        triggerTies(triggers[item]);
    }
    autoAdaptTip();
};

function contain(array, ele) {
    for (var i = 0; i < array.length; i++) {
        if (array[i] === ele) {
            return true;
        }
    }
    return false;
};

function triggerTies(json) {
    for (var j = 0; j < json.length; j++) {
        for (var k in json[j]) {
            triggerAction(k, json[j][k]);
        }
    }
};

function triggerAction(ele, condition) {
    var array;
    if (condition.indexOf("&") >= 0) {
        array = condition.split("\&");
    } else {
        array = condition.split("\|");
    }
    for (var i = 0; i < array.length; i++) {
        var notEq = array[i].search("!=") >= 0 ? true : false;
        var operator = array[i].search("!=") < 0 ? "=" : "!=";
        var item = array[i].split(operator)[0];
        var val = array[i].split(operator)[1];
        if (val.indexOf("'") > -1 || val.indexOf("\"") > -1) {
            val = eval(val);
        }
        var target = $("[name='" + item + "']:selected", getRestrictedArea()).length > 0 ?
                $("[name='" + item + "']:selected", getRestrictedArea()) : $("[name='" + item + "']:checked", getRestrictedArea());
        if ($(target).length <= 0) {
            target = $("[name='" + item + "']", getRestrictedArea());
            if(target.length > 1) {
                $("#form-item-" + ele, getRestrictedArea()).hide();// 有多个选项，且未选中时，则隐藏
                continue;
            }
        }
        if ($(target).length <= 0) {
            continue;
        }
        if (condition.indexOf("&") >= 0) {
            if (compareVal($(target).val(), val, !notEq)) {
                $("#form-item-" + ele, getRestrictedArea()).hide();
                return;
            }
        } else {
            if (compareVal($(target).val(), val, notEq)) {
                $("#form-item-" + ele, getRestrictedArea()).fadeIn(200);
                return;
            }
        }
    }
    if (condition.indexOf("&") >= 0) {
        $("#form-item-" + ele, getRestrictedArea()).fadeIn(200);
    } else {
        $("#form-item-" + ele, getRestrictedArea()).hide();
    }
};

function compareVal(value, val, notEq) {
    if (notEq) {
        return value !== val;
    }
    return value === val;
};

function effectiveInfoFields(conditions) {
    var triggers = {};
    for (var key in conditions) {
        var condition = conditions[key];
        var array;
        if (condition.indexOf("&") >= 0) {
            array = condition.split("\&");
        } else {
            array = condition.split("\|");
        }

        for (var i = 0; i < array.length; i++) {
            var operator = array[i].search("!=") < 0 ? "=" : "!=";
            var triggerItem = array[i].split(operator)[0];
            if (!triggers[triggerItem]) {
                triggers[triggerItem] = [];
            }
            var json = {};
            json[key] = condition;
            if (!contain(triggers[triggerItem], json)) {
                triggers[triggerItem].push(json);
            }
        }
    }
    for (var item in triggers) {
        for (var j = 0; j < triggers[item].length; j++) {
            for (var k in triggers[item][j]) {
                showHide(k, triggers[item][j][k]);
            }
        }
    }
}
function showHide(ele, condition) {
    var array;
    if (condition.indexOf("&") >= 0) {
        array = condition.split("\&");
    } else {
        array = condition.split("\|");
    }
    for (var i = 0; i < array.length; i++) {
        var notEq = array[i].search("!=") >= 0 ? true : false;
        var operator = array[i].search("!=") < 0 ? "=" : "!=";
        var item = array[i].split(operator)[0];
        var val = array[i].split(operator)[1];
        if (val.indexOf("'") > -1 || val.indexOf("\"") > -1) {
            val = eval(val);
        }
        var rowTarget = $("[row-item='" + item + "']", getRestrictedArea());
        if (condition.indexOf("&") >= 0) {
            if (compareVal($($("td", rowTarget)[1]).attr("field-val"), val, !notEq)) {
                $("[row-item='" + ele + "']", getRestrictedArea()).hide();
                return;
            }
        } else {
            if (compareVal($($("td", rowTarget)[1]).attr("field-val"), val, notEq)) {
                $("[row-item='" + ele + "']", getRestrictedArea()).fadeIn(200);
                return;
            }
        }
    }
    if (condition.indexOf("&") >= 0) {
        $("[row-item='" + ele + "']", getRestrictedArea()).fadeIn(200);
    } else {
        $("[row-item='" + ele + "']", getRestrictedArea()).hide();
    }
};

/**
 * 下拉列表，可输入下拉列表
 */
/*  jQuery Nice Select - v1.1.0
    https://github.com/hernansartorio/jquery-nice-select
    Made by Hernán Sartorio  */
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
            $("ul.list", this).css({"margin-top" : (0 - $("ul.list", this).height() - 38) + "px"});
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
        if ($("input[type='hidden']", $dropdown).attr("readonly") !== "readonly") {
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
};
// end

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
var tw = {
    /**
     * 页面跳转
     */
    goback: function (obj) {
        if ($("[hidden-for-goback='true']", getRestrictedArea()).length > 0) {
            $(obj).parents(".bodyDiv").prev(".breadcrumb[hidden-for-goback!='true']").remove();
            $(obj).parents(".bodyDiv").remove();
            $("[hidden-for-goback='true']", getRestrictedArea()).removeAttr("hidden-for-goback").show();
        }
    },
    formToJson: function (formSelector) {
        var paramArray = $(formSelector).serializeArray();
        var jsonObj = {};
        $(paramArray).each(function (i, element) {
            if ($.trim(element.value) !== "") {
                jsonObj[element.name] = element.value;
            }
        });
        return jsonObj;
    },
    /**
     * ajax 填充 html 片段
     * container 参数支持任意 jquery 选择器语法，例如："#id"、".content-box .edit-profile"
     */
    fill: function (url, data, container, append, element) {
        $("#mask-loading").show();
        $.ajax({
            url: url,
            data: data,
            cache: false,
            async: false,
            dataType: "html",
            type: (data && data != {} ? "POST" : "GET"),
            error: function (e) {
                handleError(e, element);
            },
            complete: function () {
                $("#mask-loading").hide();
                $(".tw-tooltip").remove();
            },
            success: function (html) {
                if (html.indexOf("<!DOCTYPE html>") >= 0 && html.indexOf("loginForm")) {
                    showConfirm(getSetting("notLogin"), {
                        "title": getSetting("pageConfirmTitle"),
                        "btn": [getSetting("reloginBtnText"), getSetting("iknowBtnText")]
                    }, function () {
                        var loginUrl = window.location.href.substring(0, window.location.href.indexOf(window.location.pathname))
                                + "/" + window.location.pathname.replace("/", "").substring(0, window.location.pathname.replace("/", "").indexOf("/"));
                        window.location.href = loginUrl;
                    }, function () {});
                    return;
                }
                if (html.indexOf("&lt;br&gt;") > 0) {
                    var array = html.split("&lt;br&gt;");
                    var newHtml = "";
                    for (var i = 0; i < array.length; i++) {
                        newHtml += array[i] + "<br>";
                    }
                    html = newHtml;
                }
                if (append) {
                    $(container).children().each(function () {
                        if (!$(this).is(":hidden")) {
                            $(this).attr("hidden-for-goback", "true").hide();
                        }
                    });
                    $(container).append(html);
                } else {
                    $(container).html(html);
                    $(".form-btn a.btn[btn-type='goback'][href='javascript:void(0);']", $(container)).parents(".block-bg").hide();
                }
                setOrReset();
                tooltip(".tooltips", {transition: true, time: 200});
                var browserInfo = browserNV();
                // 火狐浏览器特殊适配89.0版本开始浏览器通用，89版本之前，采用css中定义的hack设置
                if (browserInfo != {} && browserInfo.core === "Firefox" && browserInfo.v >= 89.0) {
                    $(".main-body", getRestrictedArea()).css({"min-height": "100%", "height": "100%"});
                } else if (browserInfo != {} && ((browserInfo.core === "Edge" && browserInfo.v <= 60.0) || (browserInfo.core === "IE" && browserInfo.v <= 11.0))) {
                    // ITAIT-4984 微软自研浏览器 Edge 样式特殊处理，解决滚动条样式问题
                    $(".main-body", getRestrictedArea()).css({"min-height": "calc(-100px + 100%)", "height": "auto", "top": "100px", "bottom": "100px"});
                }
                autoAdaptTip();
                $("form[name='pageForm'] a[tabgroup]").bind("click", function() {
                    window.setTimeout(function(){autoAdaptTip();}, 100);
                });
            }
        });
    },
    bindFill: function (selector, container, append, resetData) {
        $(selector).unbind("click").bind("click", function (e) {
            e.preventDefault();
            // e.target.href 或者 $(e.target).attr("href")
            var $this = $(this);
            var url = $this.attr("href") || $this.attr("url");
            // 跳过 href="javascript:void(0);"
            if (url.indexOf("javascript:") === 0) {
                return;
            }
            var data = resetData ? {} : tw.formToJson($("form[name='filterForm']", getRestrictedArea()));
            // 优先使用 a 标签上 fill 属性值指定的 container
            var targetContainer = $this.attr("fill") || $(container, getRestrictedArea());
            tw.fill(url, data, targetContainer, append, $this);
        });
    }
};
/* tooltip */
(function (window) {
    var bindings = [];
    function tooltip(ele, transitionObj, enterCallback, outCallback) {
        if (!ele || typeof ele !== "string") {
            console.error(new Error('The "tooltip" method requires the "class" of at least one parameter'));
            return;
        }
        var transition = transitionObj && ({}).constructor.name === "Object" && transitionObj.transition || false;
        var time = transitionObj && ({}).constructor.name === "Object" && transitionObj.time || 200, timer = null;
        while (bindings.length > 0) {
            var item = bindings.pop();
            item["el"].removeEventListener("touchstart", item["touchstart"]);
            item["el"].removeEventListener("touchend", item["touchend"]);
        }
        var tipContent = document.createElement("div");
        Array.prototype.slice.call(document.querySelectorAll(ele)).forEach(function (el) {
            var showEvent = function () {
                var pos = el.getBoundingClientRect(), currenLeft = pos.left, currenTop = pos.top, currenWidth = pos.width, 
                    currenHeight = pos.height, direction = (el.getAttribute("data-tip-arrow") || "top").replace(/_/g, "-");
                tipContentSetter(tipContent, el.getAttribute("data-tip"), direction);
                var tipContentWidth = tipContent.offsetWidth, tipContentHeight = tipContent.offsetHeight;
                switch (direction) {
                    case "top":
                        tipContent.style.left = (currenLeft + currenWidth / 2 - tipContentWidth / 2) + "px";
                        tipContent.style.top = (currenTop - tipContentHeight - 7) + "px";
                        break;
                    case "left":
                        tipContent.style.left = (currenLeft - tipContentWidth - 7) + "px";
                        tipContent.style.top = (currenTop + currenHeight / 2 - tipContentHeight / 2) + "px";
                        break;
                    case "right":
                        tipContent.style.left = (currenLeft + currenWidth + 7) + "px";
                        tipContent.style.top = (currenTop + currenHeight / 2 - tipContentHeight / 2) + "px";
                        break;
                    case "bottom":
                        tipContent.style.left = (currenLeft + currenWidth / 2 - tipContentWidth / 2) + "px";
                        tipContent.style.top = (currenTop + currenHeight + 7) + "px";
                        break;
                    case "top-left":
                        tipContent.style.left = (currenLeft - tipContentWidth + currenWidth / 2 + 7) + "px";
                        tipContent.style.top = (currenTop - tipContentHeight - 7) + "px";
                        break;
                    case "top-right":
                        tipContent.style.left = (currenLeft + currenWidth - currenWidth / 2 - 11) + "px";
                        tipContent.style.top = (currenTop - tipContentHeight - 7) + "px";
                        break;
                    case "bottom-left":
                        tipContent.style.left = (currenLeft - tipContentWidth + currenWidth / 2 + 7) + "px";
                        tipContent.style.top = (currenTop + currenHeight + 7) + "px";
                        break;
                    case "bottom-right":
                        tipContent.style.left = (currenLeft + currenWidth / 2 - 11) + "px";
                        tipContent.style.top = (currenTop + currenHeight + 7) + "px";
                }
            };
            var destroyEvent = function () {
                var oldTipContent = document.querySelector(".tw-tooltip");
                if (oldTipContent) {
                    if (transition === true) {
                        return opacityTransition(oldTipContent, "leave");
                    }
                    document.body.removeChild(oldTipContent);
                    typeof outCallback === "function" ? outCallback() : null;
                }
            };
            var trigger = el;
            if (el.hasAttribute("disabled")) {
                if (el.parentNode.getAttribute("tooltipWrapper") !== "true") {
                    var newParent = document.createElement("span");
                    newParent.setAttribute("tooltipWrapper", "true");
                    el.parentNode.insertBefore(newParent, el);
                    newParent.appendChild(el);
                    trigger = newParent;
                } else {
                    trigger = el.parentNode;
                }
            }
            trigger.onmouseenter = showEvent;
            trigger.onmouseleave = destroyEvent;
            trigger.addEventListener("touchstart", showEvent);
            trigger.addEventListener("touchend", destroyEvent);
            bindings.push({"el": trigger, "touchstart": showEvent, "touchend": destroyEvent});
        });
        function tipContentSetter(tipContent, tip, direction) {
            tipContent.innerHTML = tip.replace(/</g, "&#60;").replace(/>/g, "&#62;").replace(/"/g, "&#34;").replace(/'/g, "&#39;");
            tipContent.className = "tw-tooltip tw-tooltip-" + direction;
            document.body.appendChild(tipContent);
            if (transition === true) {
                opacityTransition(tipContent, "enter");
                return;
            }
            typeof enterCallback === "function" ? enterCallback() : null;
        }
        function opacityTransition(ele, state) {
            timer && clearTimeout(timer);
            ele.style.setProperty("transition", "opacity " + time / 1000 + "s");
            ele.style.setProperty("-webkit-transition", "opacity " + time / 1000 + "s");
            if (state === "enter") {
                ele.style.opacity = 0;
                timer = setTimeout(function () {
                    ele.style.opacity = 1;
                    typeof enterCallback === "function" ? enterCallback() : null;
                }, 0);
            } else if (state === "leave") {
                ele.style.opacity = 0;
                typeof outCallback === "function" ? outCallback() : null;
                timer = setTimeout(function () {
                    try {
                        document.body.removeChild(ele);
                    } catch (e) {
                    }
                }, time);
            }
        }
    }
    window.tooltip = tooltip;
})(window);
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/**
 * 获取全局配置项
 * @param key 配置 key
 */
function getSetting(key) {
    var settings = eval("(typeof global_setting !== 'undefined') ? global_setting : {}");
    return settings[key] ? settings[key] : key;
};
/**
 * 统一处理ajax异常
 * @param {*} e 
 */
function handleError(e, element) {
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
            }, function () {});
        } else {
            showMsg(e.status + ":" + getSetting("pageErrorMsg"), "error");
        }
    }
};
/**
 * 获取浏览器名称及版本号信息
 * @returns {} 或 {"core": "xxx", "version": "xx.x"}
 */
function browserNV() {
    var agent = navigator.userAgent;
    var regStr_ie = /MSIE [\d.]+/gi;
    var regStr_ff = /Firefox\/[\d.]+/gi;
    var regStr_edge = /Edge\/[\d.]+/gi;
    var regStr_edg = /Edg\/[\d.]+/gi;
    var regStr_chrome = /Chrome\/[\d.]+/gi;
    var regStr_saf = /Safari\/[\d.]+/gi;
    var nv = navigator.userAgent.indexOf('Trident') > -1 && navigator.userAgent.indexOf("rv:11.0") > -1 ? "IE:11.0" : "";
    if (agent.indexOf("MSIE") > 0) {// IE
        nv = agent.match(regStr_ie).toString();
    } else if (agent.indexOf("Firefox") > 0) {// Firefox
        nv = agent.match(regStr_ff).toString();
    } else if (agent.indexOf("Chrome") > 0 && agent.indexOf("Edge/") > 0) {// Edge(Microsoft)
        nv = agent.match(regStr_edge).toString();
    } else if (agent.indexOf("Chrome") > 0 && agent.indexOf("Edg/") > 0) {// Edge(Base Chrominum)
        nv = agent.match(regStr_edg).toString();
    } else if (agent.indexOf("Chrome") > 0) { // Chrome
        nv = agent.match(regStr_chrome).toString();
    } else if (agent.indexOf("Safari") > 0 && agent.indexOf("Chrome") < 0) {// Safari
        nv = agent.match(regStr_saf).toString();
    }
    if (nv.indexOf("Edg/") > -1) {
        nv = nv.replace("Edg/", "Edge/");
    }
    //Here does not display "/"
    if (nv.indexOf("Edge") !== -1 || nv.indexOf("Firefox") !== -1 || nv.indexOf("Chrome") !== -1 || nv.indexOf("Safari") !== -1) {
        nv = nv.replace("/", ":");
    }
    //Here does not display space
    if (nv.indexOf("MSIE") !== -1) {
        //MSIE replace IE & trim space
        nv = nv.replace("MSIE", "IE").replace(/\s/g, ":");
    }
    if (nv === "") {
        return {};
    }
    var v = "";
    var array = nv.split(":")[1].split(".");
    for (var i = 0; i < array.length; i++) {
        if (i === 1) {
            v += ".";
        }
        v += array[i];
    }
    return nv === "" ? {} : {"core": nv.split(":")[0], "version": nv.split(":")[1], "v": Number(v)};
};
