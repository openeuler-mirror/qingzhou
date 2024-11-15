# Qingzhou（轻舟）融合管理开发平台

## 项目简介

轻舟是一款开源的轻量级软件开发平台，其愿景是优化通用型Web管理软件的开发质量与效率，并实现不同类型软件的集中化统一管理。
基于轻舟开发Web管理软件，只需编写简单的 Java
Bean，即可自动获得业务模块对应的前端网页、REST接口、JMX接口、国际化等服务，并能开箱即用内置的用户管理、认证授权、监视自动化、文件上传下载、本地和远程集中管理、公共组件等能力。
在轻舟的架构里，管理业务系统的逻辑被封装到定制开发的应用包内，应用包可被按需地部署到轻舟平台之上，进而获得业务系统的可配置性和可观测性，从而优化企业内部复杂业务系统的管理效能。

## 环境要求

JDK >= 1.8

## 安装启动

1. **编译安装**

在项目根目录执行 `mvn clean install` 命令，然后可在项目的 `package/target/qingzhou` 得到轻舟的安装包。
> 其中：
>
> **bin**：为轻舟的可执行程序目录，包含不同平台的脚本文件等；
>
> **instances**：为轻舟的实例存放目录，包含轻舟的配置文件和应用的业务数据等；
>
> **lib**：为轻舟的程序文件，以轻舟的版本号为规划，按目录存放。

2. **启动服务**

启动轻舟有两种方式，**择其一**即可：

- 在 `${轻舟的安装包}/bin` 下，执行对应平台的 start 脚本；
- 执行命令：`java -jar ${轻舟的安装包}/bin/qingzhou-launcher.jar instance start`

> 看到类似如下的日志输出，则表示启动完成：
>
> Open a browser to access the Qingzhou console: http://localhost:9000/console

3. **访问控制台**

打开浏览器访问轻舟的管理平台：
[http://localhost:9000/console](http://localhost:9000/console)

> 注：
>
> 若遇到浏览器页面提示 IP 不受信任，可设置轻舟的配置文件`${轻舟的安装包}/instances/instance1/conf/qingzhou.json`，
> 修改其中的 trustedIp 值，指定一个浏览器IP正则表达式来信任特定的浏览器，设置为 * 表示信任所有浏览器。

## 鸣谢

本项目借鉴和引用了一些优秀项目的设计思路或代码库文件，在此特别感谢原作者的贡献与付出，同时也感谢众多小伙伴们提出项目问题、贡献代码等！

相关项目：

【前端】

+ Muuri ([https://github.com/haltu/muuri](https://github.com/haltu/muuri))
+ marked ([https://github.com/markedjs/marked](https://github.com/markedjs/marked))
+ Multiple Select ([http://multiple-select.wenzhixin.net.cn](http://multiple-select.wenzhixin.net.cn))
+ Layui layer ([https://gitee.com/layui/layer](https://gitee.com/layui/layer))
+ ZUI ([https://openzui.com](https://openzui.com))
+ jQuery ([https://jquery.com](https://jquery.com))
+ Date Range Picker ([https://daterangepicker.com/])

【后端】

+
QR-Code-generator ([https://www.nayuki.io/page/qr-code-generator-library](https://www.nayuki.io/page/qr-code-generator-library))
+ Apache Tomcat ([https://tomcat.apache.org](https://tomcat.apache.org))
+ Apache MINA SSHD ([https://mina.apache.org/sshd-project/](https://mina.apache.org/sshd-project/))
+ Gson ([https://github.com/google/gson](https://github.com/google/gson))
+ tinylog ([https://tinylog.org](https://tinylog.org))
+ Javassist ([http://www.javassist.org](http://www.javassist.org))

## 参与贡献

贡献让开源社区成为一个非常适合学习、启发和创新的地方，您所作出的任何贡献都是**受人尊敬**的。

如果您有好的建议，请复刻（fork）本仓库并且创建一个拉取请求（pull request）。
您也可以简单的创建一个议题（issue），并且添加标签『enhancement』。 不要忘记给项目点一个star！再次感谢！

1. Fork [本仓库](https://gitee.com/openeuler/qingzhou)
2. 新建您的 Feature 分支(`git checkout -b feature/AmazingFeature`)
3. 提交您的变更(`git commit -m 'Add some AmazingFeature'`)
4. 推送到该分支(`git push origin feature/AmazingFeature`)
5. 创建拉取请求(Pull Request)
