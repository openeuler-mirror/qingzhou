;!function (window, undefined) {
    var qzCls = {};
    qzCls.tab = function (dom) {
        this.rootBox = $(dom).length > 0 ? $(dom) : document.body;
        var that = this;
        this.init = function (dom, minFn, destroyFn) {
            if ($(dom, this.rootBox).length > 0) {
                var html = ""
                    + "<div class=\"tab-wrapper\">"
                    + "    <div class=\"tab-header\">"
                    + "        <button class=\"tab-btn btn-left\"><i><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"25\" height=\"25\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M11.0002 17.035C11.0002 17.1383 10.9682 17.239 10.9087 17.3234C10.7494 17.549 10.4375 17.6028 10.2119 17.4435L3.07889 12.4085C3.03228 12.3756 2.99164 12.335 2.95874 12.2883C2.7995 12.0627 2.85329 11.7508 3.07889 11.5915L10.2119 6.55648C10.2962 6.49693 10.3969 6.46497 10.5002 6.46497C10.7763 6.46497 11.0002 6.68882 11.0002 6.96497V17.035ZM12.0789 12.4085C12.0323 12.3756 11.9916 12.335 11.9587 12.2883C11.7995 12.0627 11.8533 11.7508 12.0789 11.5915L19.2119 6.55648C19.2962 6.49693 19.3969 6.46497 19.5002 6.46497C19.7763 6.46497 20.0002 6.68882 20.0002 6.96497V17.035C20.0002 17.1383 19.9682 17.239 19.9087 17.3234C19.7494 17.549 19.4375 17.6028 19.2119 17.4435L12.0789 12.4085Z\"></path></svg></i></button>"
                    + "        <nav class=\"tab-nav\">"
                    + "            <div class=\"tab-nav-box\"></div>"
                    + "        </nav>"
                    + "        <button class=\"tab-btn btn-right\"><i><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"25\" height=\"25\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M4.78834 17.4434C4.70398 17.503 4.60326 17.535 4.5 17.535C4.22386 17.535 4 17.3111 4 17.035V6.96488C4 6.86163 4.03197 6.7609 4.09152 6.67654C4.25076 6.45094 4.56274 6.39715 4.78834 6.5564L11.9213 11.5914C11.9679 11.6243 12.0086 11.665 12.0415 11.7116C12.2007 11.9372 12.1469 12.2492 11.9213 12.4084L4.78834 17.4434ZM13 6.96488C13 6.86163 13.032 6.7609 13.0915 6.67654C13.2508 6.45094 13.5627 6.39715 13.7883 6.5564L20.9213 11.5914C20.9679 11.6243 21.0086 11.665 21.0415 11.7116C21.2007 11.9372 21.1469 12.2492 20.9213 12.4084L13.7883 17.4434C13.704 17.503 13.6033 17.535 13.5 17.535C13.2239 17.535 13 17.3111 13 17.035V6.96488Z\"></path></svg></i></button>"
                    + "        <button class=\"tab-btn mintab\"><i><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"22\" height=\"22\"><path d=\"M15 4.00008H13V11.0001H20V9.00008H16.4142L20.7071 4.70718L19.2929 3.29297L15 7.58586V4.00008ZM4.00008 15H7.58586L3.29297 19.2929L4.70718 20.7071L9.00008 16.4142V20H11.0001V13H4.00008V15Z\"></path></svg></i></button>"
                    + "        <button class=\"tab-btn destroy\"><i><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"22\" height=\"22\"><path d=\"M11.9997 10.5865L16.9495 5.63672L18.3637 7.05093L13.4139 12.0007L18.3637 16.9504L16.9495 18.3646L11.9997 13.4149L7.04996 18.3646L5.63574 16.9504L10.5855 12.0007L5.63574 7.05093L7.04996 5.63672L11.9997 10.5865Z\"></path></svg></i></button>"
                    + "    </div>"
                    + "    <div class=\"tab-body\"></div>"
                    + "</div>";
                $(html).appendTo($(dom, this.rootBox));
            }

            /*左按钮事件*/
            $("div.tab-header>.btn-left", this.rootBox).unbind("click").bind("click", function (e) {
                e.preventDefault();
                that.moveLeft(this);
            });
            /*右按钮事件*/
            $("div.tab-header>.btn-right", this.rootBox).unbind("click").bind("click", function (e) {
                e.preventDefault();
                that.moveRight(this);
            });
            if (typeof minFn === "function") {
                $("div.tab-header>.mintab", this.rootBox).unbind("click").bind("click", function (e) {
                    $(".tab-main", this.rootBox).each(function () {
                        DashboardManager.hide($(this));
                    });
                    e.preventDefault();
                    minFn.call(null);
                });
            }
            if (typeof destroyFn === "function") {
                $("div.tab-header>.destroy", this.rootBox).unbind("click").bind("click", function (e) {
                    DashboardManager.close($("div.tab-container", getRestrictedArea()));

                    e.preventDefault();
                    destroyFn.call(null);
                });
            }
            this.bindTabEvent();
            return this;
        };

        this.bindTabEvent = function () {
            /*选项卡切换事件*/
            $(".tab-nav-box .tab-nav-item", this.rootBox).unbind("click").bind("click", function (e) {
                e.preventDefault();
                that.activeTab(this);
            });
            /*选项卡关闭事件*/
            $(".tab-nav-box .tab-nav-item i", this.rootBox).unbind("click").bind("click", function (e) {
                e.preventDefault();
                that.closeTab($(this).parents(".tab-nav-item"));
            });
            /*选项卡双击关闭事件*/
            $(".tab-nav-box .tab-nav-item", this.rootBox).unbind("dblclick").bind("dblclick", function (e) {
                e.preventDefault();
                that.closeTab(this);
            });
            return this;
        };

        this.addTab = function (dataId, title, html, options, closeable, callback) {
            var exist = $(".tab-nav-item[data-id='" + dataId + "']", this.rootBox).length > 0;
            if (!exist) {
                $(".tab-nav-item.active", this.rootBox).removeClass("active");
                var tab = "<span class='tab-nav-item active' data-id='" + dataId + "' ";
                if (typeof options === "object") {
                    for (var k in options) {
                        tab += " " + k + "='" + options[k] + "'";
                    }
                }
                tab += ">" + title;
                if (closeable) {
                    tab += "<i><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22ZM12 20C16.4183 20 20 16.4183 20 12C20 7.58172 16.4183 4 12 4C7.58172 4 4 7.58172 4 12C4 16.4183 7.58172 20 12 20ZM12 10.5858L14.8284 7.75736L16.2426 9.17157L13.4142 12L16.2426 14.8284L14.8284 16.2426L12 13.4142L9.17157 16.2426L7.75736 14.8284L10.5858 12L7.75736 9.17157L9.17157 7.75736L12 10.5858Z\"></path></svg></i>";
                }
                tab += " </span>";
                $(".tab-nav-box", this.rootBox).append(tab);
                var tabMain = $(".tab-body div.tab-main", this.rootBox);
                tabMain.hide();
                DashboardManager.hide(tabMain);
                $(".tab-body", this.rootBox).append("<div class='tab-main' style=\"min-height:100px;\" data-id='" + dataId + "'>" + html + "</div>");
                this.bindTabEvent();
            }
            this.activeTab($(".tab-nav-item.active", this.rootBox));
            if (!exist && (typeof callback) === "function") {
                callback.call(null, $(".tab-body>div.tab-main[data-id='" + dataId + "']", this.rootBox));
            }
            return this;
        };

        this.activeTab = function (cur) {
            var dataId = $(cur).attr("data-id");
            $(".tab-main", this.rootBox).each(function () {
                if ($(this).attr("data-id") === dataId) {
                    DashboardManager.restore($(this));
                }else{
                    DashboardManager.hide($(this));
                }
            });
            if (!$(cur).hasClass("active")) {
                $(".tab-main", this.rootBox).each(function () {
                    if ($(this).attr("data-id") === dataId) {
                        $(this).show().siblings(".tab-main").hide();
                        return false;
                    }
                });
                $(cur).addClass("active").siblings(".tab-nav-item").removeClass("active");
            }
            var prev_all = this.calcWidth($(cur).prevAll()), next_all = this.calcWidth($(cur).nextAll());
            var other_width = this.calcWidth($(".tab-header").children().not(".tab-nav"));
            var navWidth = $(".tab-header", this.rootBox).outerWidth(true) - other_width;//可视宽度
            var hidewidth = 0;
            if ($(".tab-nav-box", this.rootBox).width() < navWidth) {
                hidewidth = 0;
            } else {
                if (next_all <= (navWidth - $(cur).outerWidth(true) - $(cur).next().outerWidth(true))) {
                    if ((navWidth - $(cur).next().outerWidth(true)) > next_all) {
                        hidewidth = prev_all;
                        var m = cur;
                        while ((hidewidth - $(m).outerWidth()) > ($(".tab-nav-box", this.rootBox).outerWidth() - navWidth)) {
                            hidewidth -= $(m).prev().outerWidth();
                            m = $(m).prev();
                        }
                    }
                } else {
                    if (prev_all > (navWidth - $(cur).outerWidth(true) - $(cur).prev().outerWidth(true))) {
                        hidewidth = prev_all - $(cur).prev().outerWidth(true);
                    }
                }
            }
            $(".tab-nav-box", this.rootBox).animate({"marginLeft": 0 - hidewidth + "px"}, "fast");
            return this;
        };
        
        this.getActiveTab = function() {
            return $("div.tab-main[data-id='" + $(".tab-nav-item.active", this.rootBox).attr("data-id") + "']", this.rootBox);
        };

        this.calcWidth = function (tabs) {
            var sumWidth = 0;
            $(tabs).each(function () {
                sumWidth += $(this).outerWidth(true);
            });
            return sumWidth;
        };

        this.moveLeft = function () {
            var ml = Math.abs(parseInt($(".tab-nav-box", this.rootBox).css("margin-left")));
            var other_width = this.calcWidth($(".tab-header", this.rootBox).children().not(".tab-nav"));
            var navWidth = $(".tab-header", this.rootBox).outerWidth(true) - other_width;//可视宽度
            var hidewidth = 0;
            if ($(".tab-nav-box", this.rootBox).width() < navWidth) {
                return false;
            } else {
                var tabIndex = $(".tab-nav-item:first", this.rootBox);
                var n = 0;
                while ((n + $(tabIndex).outerWidth(true)) <= ml) {
                    n += $(tabIndex).outerWidth(true);
                    tabIndex = $(tabIndex).next();
                }
                n = 0;
                if (this.calcWidth($(tabIndex).prevAll()) > navWidth) {
                    while ((n + $(tabIndex).outerWidth(true)) < (navWidth) && tabIndex.length > 0) {
                        n += $(tabIndex).outerWidth(true);
                        tabIndex = $(tabIndex).prev();
                    }
                    hidewidth = this.calcWidth($(tabIndex).prevAll());
                }
            }
            $(".tab-nav-box", this.rootBox).animate({"marginLeft": 0 - hidewidth + "px"}, "fast");
        };

        this.moveRight = function () {
            var ml = Math.abs(parseInt($(".tab-nav-box", this.rootBox).css("margin-left")));
            var other_width = this.calcWidth($(".tab-header", this.rootBox).children().not(".tab-nav"));
            var navWidth = $(".tab-header", this.rootBox).outerWidth(true) - other_width;//可视宽度
            var hidewidth = 0;
            if ($(".tab-nav-box", this.rootBox).width() < navWidth) {
                return false;
            } else {
                var tabIndex = $(".tab-nav-item:first", this.rootBox);
                var n = 0;
                while ((n + $(tabIndex).outerWidth(true)) <= ml) {
                    n += $(tabIndex).outerWidth(true);
                    tabIndex = $(tabIndex).next();
                }
                n = 0;
                while ((n + $(tabIndex).outerWidth(true)) < (navWidth) && tabIndex.length > 0) {
                    n += $(tabIndex).outerWidth(true);
                    tabIndex = $(tabIndex).next();
                }
                hidewidth = this.calcWidth($(tabIndex).prevAll());
                if (hidewidth > 0) {
                    $(".tab-nav-box", this.rootBox).animate({"marginLeft": 0 - hidewidth + "px"}, "fast");
                }
            }
        };

        this.closeTab = function (ele) {
            if ($("i", ele).length === 0) {
                return;
            }
            var eleDataId = $(ele).attr("data-id");
            var cur_width = $(ele).width();
            if ($(ele).hasClass("active")) {
                if ($(ele).next(".tab-nav-item").size() > 0) {
                    var dataId = $(ele).next(".tab-nav-item:eq(0)").attr("data-id");
                    $(ele).next(".tab-nav-item:eq(0)").addClass("active");
                    $(".tab-main", this.rootBox).each(function () {
                        if ($(this).attr("data-id") === dataId) {
                            DashboardManager.close($(this));
                            $(this).show().siblings(".tab-main").hide();
                            return false;
                        }
                    });
                    var n = parseInt($(".tab-nav-box", this.rootBox).css("margin-left"));
                    if (n < 0) {
                        $(".tab-nav-box", this.rootBox).animate({"marginLeft": (n + cur_width) + "px"}, "fast");
                    }
                    $(ele).remove();
                    $(".tab-main", this.rootBox).each(function () {
                        if ($(this).attr("data-id") === eleDataId) {
                            DashboardManager.close($(this));
                            $(this).remove();
                            return false;
                        }
                    });
                } else if ($(ele).prev(".tab-nav-item").size() > 0) {
                    var dataId = $(ele).prev(".tab-nav-item:last").attr("data-id");
                    $(ele).prev(".tab-nav-item:last").addClass("active");
                    $(".tab-main", this.rootBox).each(function () {
                        if ($(this).attr("data-id") === dataId) {
                            DashboardManager.close($(this));
                            $(this).show().siblings(".tab-main").hide();
                            return false;
                        }
                    });
                    $(ele).remove();
                    $(".tab-main", this.rootBox).each(function () {
                        if ($(this).attr("data-id") === eleDataId) {
                            DashboardManager.close($(this));
                            $(this).remove();
                            return false;
                        }
                    });
                } else {
                    $(ele).remove();
                    var tabMain = $(".tab-main[data-id='" + eleDataId + "']", this.rootBox);
                    tabMain.remove();
                    DashboardManager.close(tabMain);
                }
            } else {
                $(ele).remove();
                $(".tab-main", this.rootBox).each(function () {
                    if ($(this).attr("data-id") === eleDataId) {
                        DashboardManager.close($(this));
                        $(this).remove();
                        return false;
                    }
                });
                this.activeTab($(".tab-nav-item.active", this.rootBox));
            }
            if ($("div.tab-nav-box>span.tab-nav-item").length === 0) {
                $("div.tab-header>.destroy", this.rootBox).click();
            }
            return false;
        };
    };

    qzCls.tooltip = function (ele, transitionObj, enterCallback, outCallback) {
        if (!ele || typeof ele !== "string") {
            console.error(new Error('The "tooltip" method requires the "class" of at least one parameter'));
            return;
        }
        this.bindings = [];
        var that = this;
        var transition = transitionObj && ({}).constructor.name === "Object" && transitionObj.transition || false;
        var time = transitionObj && ({}).constructor.name === "Object" && transitionObj.time || 200, timer = null;
        while (this.bindings.length > 0) {
            var item = this.bindings.pop();
            item["el"].removeEventListener("touchstart", item["touchstart"]);
            item["el"].removeEventListener("touchend", item["touchend"]);
        }
        var tipContent = document.createElement("div");
        Array.prototype.slice.call(document.querySelectorAll(ele)).forEach(function (el) {
            var dataTip = el.getAttribute("data-tip");
            if (dataTip == undefined || dataTip == null || dataTip === "null" || dataTip === "undefined") {
                return;
            }
            var showEvent = function () {
                var pos = el.getBoundingClientRect(), currenLeft = pos.left, currenTop = pos.top,
                    currenWidth = pos.width,
                    currenHeight = pos.height,
                    direction = (el.getAttribute("data-tip-arrow") || "top").replace(/_/g, "-");
                tipContentSetter(tipContent, dataTip, direction);
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
                var oldTipContent = document.querySelector(".qz-tooltip");
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
            that.bindings.push({"el": trigger, "touchstart": showEvent, "touchend": destroyEvent});
        });

        function tipContentSetter(tipContent, tip, direction) {
            tipContent.innerHTML = (tip == null || tip === undefined ? "" : tip).replace(/</g, "&#60;").replace(/>/g, "&#62;").replace(/"/g, "&#34;").replace(/'/g, "&#39;");
            tipContent.className = "qz-tooltip qz-tooltip-" + direction;
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
    };

    window.qz = {
        version: "1.0",
        author: "https://www.tongtech.com",
        tab: function (dom) {
            return new qzCls.tab(dom);
        },
        tooltip: function (ele, transitionObj, enterCallback, outCallback) {
            return new qzCls.tooltip(ele, transitionObj, enterCallback, outCallback);
        },
        guid: function () {
            function S4() {
                return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
            }

            return (S4() + S4() + S4() + S4() + S4() + S4() + S4() + S4());
        },
        randomStr: function (length) {
            length = length ? length : 6;
            var random = "";
            var chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
            for (var i = length; i > 0; --i) {
                random += chars[Math.floor(Math.random() * chars.length)];
            }
            return random;
        },
        /**
         * 获取浏览器名称及版本号信息
         * @returns {} 或 {"core": "xxx", "version": "xx.x"}
         */
        browserNV: function () {
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
        },
        formToJson: function (formSelector) {
            var jsonObj = {};
            return $(formSelector).serializeArray();
        },
        /**
         * 为目标对象绑定事件及响应结果目标渲染对象
         * @param {type} triggerSelector 触发事件的元素选择器表达式
         * @param {type} targetSelector 响应结果的目标渲染元素
         * @param {type} append （true 表示在目标渲染元素中追加响应结果 | false 表示覆盖目标渲染元素的内容。通常情况下请用 false）
         * @param {type} resetData (列表页搜索条件是否需要重置搜索条件，用于搜索、分页、列表数据刷新等)
         * @param {type} restrictedArea 限定区域元素，此参数若省去，则默认为 document.body (注：此参数的作用主要是限制选择响应结果的目标渲染元素，避免获取到多个目标元素导致渲染异常)
         * @returns {unresolved}
         */
        bindFill: function (triggerSelector, targetSelector, append, resetData, restrictedArea, afterRenderCall) {
            $(triggerSelector + "[action-type='sub_form']", restrictedArea).each(function () {
                var actionType = $(this).attr("action-type");
                var actionTypeMethod = bindingActions[actionType];
                actionTypeMethod.call(null, null, triggerSelector + "[action-type='sub_form']", false, restrictedArea);
            });

            var that = this;
            $(triggerSelector + "[loaded!='true']", restrictedArea).attr("loaded", "true").click(function (e) {
                e.preventDefault();
                // 跳过 href="javascript:void(0);"
                var url = $(this).attr("href") || $(this).attr("url");
                var actionType = $(this).attr("action-type");
                if (url.indexOf("javascript:") === 0) {
                    return;
                }
                var data = resetData ? {} : that.formToJson($("form[name='filterForm']", restrictedArea || document.body));
                if (actionType !== undefined && actionType === getSetting("downloadView")) {
                    url += "?";
                    for (const item of data) {
                        url += item.name + "=" + item.value + "&";
                    }
                    window.location.href = url;
                    return;
                }
                if (actionType !== undefined && actionType === getSetting("back")) {
                    var modelname = $(this).attr("modelname")
                    var menuLink = $("li.treeview a[modelname='" + modelname + "']", restrictedArea);
                    var sidebar = $("aside.main-sidebar", restrictedArea);
                    $(".menu-open", sidebar).removeClass("menu-open").find(".treeview-menu").hide();
                    $("ul li.treeview.active", sidebar).removeClass("active");
                    menuLink.parent().addClass("active").parents("li.treeview").addClass("menu-open").children(".treeview-menu").show();
                }

                that.fill(url, data, $(targetSelector, restrictedArea), append, afterRenderCall);
            });
        },
        /**
         * ajax 填充 html 片段
         * container 参数支持任意 jquery 选择器语法，例如："#id"、".content-box .edit-profile"
         */
        fill: function (url, data, container, append, callback) {
            var that = this;
            $("#mask-loading").show();
            $.ajax({
                url: url,
                data: data,
                cache: false,
                async: false,
                dataType: "html",
                type: (data && data != {} ? "POST" : "GET"),
                error: function (e) {
                    handleError(e);
                },
                complete: function () {
                    $("#mask-loading").hide();
                    $(".qz-tooltip").remove();
                },
                success: function (html) {
                    if (html.indexOf("<!DOCTYPE html>") >= 0 && html.indexOf("loginForm") > -1) {
                        showConfirm(getSetting("notLogin"), {
                            "title": getSetting("pageConfirmTitle"),
                            "btn": [getSetting("reloginBtnText"), getSetting("iknowBtnText")]
                        }, function () {
                            var loginUrl = window.location.href.substring(0, window.location.href.indexOf(window.location.pathname))
                                + "/" + window.location.pathname.replace("/", "").substring(0, window.location.pathname.replace("/", "").indexOf("/"));
                            window.location.href = loginUrl;
                        }, function () {
                        });
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
                        $(container).append(html);
                    } else {
                        $(container).html(html);
                    }
                    setOrReset(container);
                    if ((typeof callback) === "function") {
                        callback.call(null, container);
                    }
                    that.tooltip(".tooltips", {transition: true, time: 200});
                    var browserInfo = qz.browserNV();
                    // 火狐浏览器特殊适配89.0版本开始浏览器通用，89版本之前，采用css中定义的hack设置
                    if (browserInfo != {} && browserInfo.core === "Firefox" && browserInfo.v >= 89.0) {
                        $(".main-body", getRestrictedArea()).css({"min-height": "100%", "height": "100%"});
                    } else if (browserInfo != {} && ((browserInfo.core === "Edge" && browserInfo.v <= 60.0) || (browserInfo.core === "IE" && browserInfo.v <= 11.0))) {
                        // ITAIT-4984 微软自研浏览器 Edge 样式特殊处理，解决滚动条样式问题
                        $(".main-body", getRestrictedArea()).css({
                            "min-height": "calc(-100px + 100%)",
                            "height": "auto",
                            "top": "100px",
                            "bottom": "100px"
                        });
                    }
                    autoAdaptTip();
                    $("form[name='pageForm'] a[tabgroup]").bind("click", function () {
                        window.setTimeout(function () {
                            autoAdaptTip();
                        }, 100);
                    });
                }
            });
        }
    };
}(window);
