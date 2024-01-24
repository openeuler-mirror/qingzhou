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
    <script src="<%=contextPath%>/static/js/jquery.min.js"></script>
    <%@ include file="../fragment/head.jsp" %>
</head>
<body>
    <div style="padding: 30px;">
        <div id="markedShow"></div>
    </div>
    <textarea id="markedText" style="display:none;">
轻舟Web管理软件开发平台
========================

轻舟，提供了一套开发Web管理软件的基础平台，基于声明式API、充血模型等思想，开发者只需编写后端业务模型代码，即可自动获得前端页面，并自动化实现参数校验、REST接口、远程管理、角色权限、页面国际化、云上对接等能力。

软件架构
-------------------

![软件架构](static/images/architecture.jpg)

内置功能
-------------

+ 服务管理

+ 应用管理

+ 角色权限管理

免责声明
-------------

本项目基于[MulanPSL-2.0](http://license.coscl.org.cn/MulanPSL2)开源许可协议，代码免费且已开源。使用时请遵循相关开源许可协议!

+ 不得将 qingzhou 用于危害国家安全、荣誉和利益的行为，不能以任何形式用于非法为目的的行为,否则后果自负
+ 虽然本项目在开发过程中很注重安全，但是您仍然需要了解：是软件皆有漏洞，任何人都无法保证软件100%没有漏洞。所以由本软件漏洞造成损失不予赔偿，同时也不承担任何因使用本软件而产生的相关法律责任。也请在软件上线前进行必要的安全监测，避免安全问题发生。

鸣谢
-------------

本项目开发过程中，借鉴和引用了许多优秀项目的设计思路或代码库文件等，在此特别感谢原作者的贡献付出！同时也感谢众多小伙伴提出项目问题及贡献的代码.

主要引用到的项目：
+ tomcat (https://tomcat.apache.org/)

+ jquery (https://jquery.com/)

+ openzui (https://openzui.com/)

+ layer (https://gitee.com/layui/layer.git)

+ multiple-select (http://multiple-select.wenzhixin.net.cn)

+ marked (https://github.com/markedjs/marked/)
        
    </textarea>

    <script src="<%=contextPath%>/static/lib/marked/marked.min.js"></script>
    <script type="text/javascript">
        document.getElementById("markedShow").innerHTML = marked.parse(document.getElementById("markedText").value);
    </script>
</body>
</html>