<%@ page pageEncoding="UTF-8" %>

<style>
    #manul {
        left: 50%;
        transform: translateX(-50%);
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
        width: 900px;
        z-index: 999;
        position: absolute;
    }

    #manul-image {
        position: relative;
        width: 100%;
        height: 500px;
    }

    #manul-image img {
        width: 100%;
        height: 100%;
        object-fit: contain;
        position: absolute;
        top: 0;
        left: 0;
        opacity: 0;
        transition: opacity 0.5s ease-in-out;
    }

    #manul-image img.active {
        opacity: 1;
    }

    .carousel-controls {
        display: flex;
        justify-content: left;
        padding: 10px;
        background-color: rgba(0, 0, 0, 0.5);
        position: absolute;
        bottom: 0;
        width: 100%;
        box-sizing: border-box;
    }

    .carousel-button {
        font-size: 16px;
        background-color: transparent;
        background-color: #4CAF50;
        color: white;
        cursor: pointer;
        width: 70px;
        height: 30px;
        z-index: 10;
    }

    .close-button {
        font-size: 16px;
        background-color: #4CAF50;
        color: white;
        width: 60px;
        height: 30px;
        cursor: pointer;
    }
</style>
<%
    String imagePath = getServletContext().getRealPath("/static/images/manul");
    File folder = new File(imagePath);
    List<String> imageNames = new ArrayList<>();
    if (folder.exists()) {
        File[] imageFiles = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowerCaseName = name.toLowerCase();
                return lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg") ||
                        lowerCaseName.endsWith(".png") || lowerCaseName.endsWith(".gif") ||
                        lowerCaseName.endsWith(".bmp");
            }
        });
        if (imageFiles != null) {
            for (File file : imageFiles) {
                imageNames.add(contextPath + "/static/images/manul/" + file.getName());
            }
        }
    }
%>
<div id="manul" style="display:none;">
    <div id="manul-image">
        <%
            // 假设 imageNames 是一个包含图片路径的 List
            for (int i = 0; i < imageNames.size(); i++) { %>
        <img src="<%= imageNames.get(i) %>" id="image<%= i %>" class="<%= i == 0 ? "active" : "" %>">
        <%
            }
        %>
    </div>
    <div class="carousel-controls">
        <button class="carousel-button prev" onclick="changeImage(-1)">上一步</button>
        <button class="carousel-button next" onclick="changeImage(1)">下一步</button>
        <button class="close-button" onclick="closeManul()">关闭</button>
    </div>
</div>

<header class="main-header">
    <nav class="navbar navbar-fixed-top">
        <%-- 顶部 左侧 logo --%>
        <div class="navbar-header">
            <a class="navbar-toggle" href="javascript:void(0);" data-toggle="collapse" data-target=".navbar-collapse"><i
                    class="icon icon-th-large"></i></a>
            <a class="sidebar-toggle" href="javascript:void(0);" data-toggle="push-menu"><i
                    class="icon icon-sliders"></i></a>
            <a class="navbar-brand" href="javascript:void(0);">
                <img src="<%=contextPath%>/static/images/login/top_logo.svg" class="logo" alt="">
                <span class="logo-mini" data-toggle="push-menu" style="display: none;">
                    <i class="icon icon-sliders"></i>
                </span>
            </a>
        </div>

        <%-- 顶部 右侧 按钮 --%>
        <div class="collapse navbar-collapse">
            <div>
                <ul class="nav navbar-nav">
                    <li><span class="console-name"><%=I18n.getKeyI18n("page.index")%></span>
                    </li>
                </ul>
                <ul class="nav navbar-nav navbar-right">
                    <li>
                        <a
                                onclick="openManul()"
                                class="tooltips"
                                data-tip='<%=I18n.getKeyI18n( "page.manul")%>'
                                data-tip-arrow="bottom">
                            <span class="circle-bg"><i class="icon icon-question"></i></span>
                        </a>
                    </li>
                    <%-- 明暗主题切换 --%>
                    <li id="switch-mode" class="switch-btn">
                        <a id="switch-mode-btn"
                           href="javascript:void(0);"
                           theme="<%= themeMode == null ? "" : themeMode %>"
                           themeUrl="<%=RESTController.encodeURL(response, contextPath + Theme.URI_THEME + "/" + ((themeMode == null || themeMode.isEmpty()) ? "dark" : ""))%>"
                           class="tooltips" data-tip="<%=I18n.getKeyI18n("page.thememode")%>" data-tip-arrow="bottom">
                        <span class="circle-bg">
                            <i class="icon <%=(themeMode == null || themeMode.isEmpty()) ? "icon-moon" : "icon-sun"%>"></i>
                        </span>
                        </a>
                    </li>
                    <%-- 切换语言 --%>
                    <li id="switch-lang" class="dropdown">
                        <a href="javascript:void(0);" class="tooltips" data-toggle="dropdown" data-tip-arrow="bottom"
                           data-tip='<%=I18n.getKeyI18n( "page.lang.switch")%>'>
                            <span class="circle-bg"><i class="icon icon-language"></i></span>
                        </a>
                        <ul class="dropdown-menu dropdown-menu-reset">
                            <%
                                for (Lang lang : Lang.values()) {
                                    out.print("<li>");
                                    out.print(String.format("<a href=\"%s\"><span>%s</span></a>", RESTController.encodeURL(response, contextPath + I18n.LANG_SWITCH_URI + "/" + lang), lang.info));
                                    out.print("</li>");
                                }
                            %>
                        </ul>
                    </li>
                    <%-- 用户/修改密码 --%>
                    <li>
                        <a id="reset-password-btn"
                           href="<%=RESTController.encodeURL( response, (contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath) + DeployerConstants.REST_PREFIX + "/" + HtmlView.FLAG + "/" + DeployerConstants.APP_SYSTEM +"/" + DeployerConstants.MODEL_PASSWORD + "/" + Update.ACTION_EDIT)%>"
                           class="tooltips" data-tip='<%=LoginManager.getLoggedUser(session).getName()%>'
                           data-tip-arrow="bottom">
                            <span class="circle-bg">
                                <i class="icon icon-<%=SystemController.getModelInfo(DeployerConstants.APP_SYSTEM, DeployerConstants.MODEL_USER).getIcon()%>"></i>
                            </span>
                        </a>
                    </li>
                    <%-- 注销 --%>
                    <li>
                        <a id="logout-btn"
                           href="<%=RESTController.encodeURL( response, contextPath + LoginManager.LOGIN_PATH + "?" + LoginManager.LOGOUT_FLAG)%>"
                           class="tooltips"
                           data-tip='<%=I18n.getKeyI18n( "page.invalidate")%>'
                           data-tip-arrow="bottom">
                            <span class="circle-bg"><i class="icon icon-signout"></i></span>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </nav>
    <script>
        const manul = document.getElementById('manul');

        // 添加点击事件监听器，点击外部区域隐藏div
        document.addEventListener('click', function (event) {
            if (!manul.contains(event.target)) {
                manul.style.display = 'none';
            }
        });

        // 阻止点击 div 内部时关闭
        manul.addEventListener('click', function (event) {
            event.stopPropagation(); // 阻止事件传播，避免触发 document 的点击事件
        });

        // 打开手册
        function openManul() {
            event.stopPropagation();
            manul.style.display = 'block';
        }

        // 当前显示的图片索引
        let currentIndex = 0;
        const images = document.querySelectorAll("#manul-image img");

        // 切换图片函数
        function changeImage(direction) {
            images[currentIndex].classList.remove("active");

            currentIndex += direction;

            if (currentIndex >= images.length) {
                currentIndex = 0;
            } else if (currentIndex < 0) {
                currentIndex = images.length - 1;
            }

            images[currentIndex].classList.add("active");
        }

        // 关闭手册函数
        function closeManul() {
            manul.style.display = 'none';
        }
    </script>
</header>
