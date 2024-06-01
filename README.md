# Qingzhou（轻舟）Web管理软件开发平台

## 平台简介

轻舟是一款开源的轻量级软件开发平台，可用于简化Web管理控制台的开发。

### 主要特性

轻舟具有免前端开发、接口多样化、支持上云等特点，预置了很多Web管理常用的功能，如认证授权、漏洞防护、国际化、菜单定制等，更重要的是，它天然支持在分布式环境下对各业务系统实施集中式的管理。

### 运行环境

- JDK >= 1.8

## 安装使用

1. 获取安装包【可选】：在项目根目录执行 `mvn clean package` 命令，之后在 `package/qingzhou/target/qingzhou/qingzhou` 可得到安装包。
2. 启动平台服务：进入安装包根目录，在 bin 目录下，根据操作系统平台执行对应的 start 脚本即开始启动，也可选择执行与平台无关的 java 命令（`java -jar ./qingzhou-launcher.jar server start`）来启动，看到类似如下的日志输出则表示启动完成：`Open a browser to access the Qingzhou console: http://localhost:9000/console`。
3.

访问控制台：平台服务启动完成后，可打开浏览器访问轻舟的可视化管理平台：[http://localhost:9000/console](http://localhost:9000/console)。

## 应用开发

基于轻舟开发应用，只需简单的几步：

1. 在 IDE 中建立一个 Java 项目工程。
2. 在工程里引入轻舟的开发工具包 `qingzhou-api`。
3. 创建一个类作应用的入口，使其实现 `qingzhou.api.QingzhouApp` 接口（该类在 `qingzhou-api`
   内，后文提到的轻舟的类也都在其内），实现 `start` 方法以定制应用的启动逻辑。`start`
   方法会接收一个 `qingzhou.api.AppContext` 的实例对象，应用通过该对象与平台交互。
4. 给入口类添加 `@App` 注解，以使得轻舟可以识别到它。
5. 创建应用的模块类，使其继承自 `qingzhou.api.ModelBase` ，对该类添加 `@Model` 注解以设置模块的名称、图标、菜单、国际化等信息。在该类内部创建 public 的属性（自动对应到页面上的表单元素），并对其添加 `@ModelField`
   注解以设置属性的相关信息。在该类内部创建方法（自动对应到页面上的按钮或链接），并对其添加 `@ModelAction`
   注解以设置方法的相关信息。关于配置的具体接口，可查看对应的Javadoc。
6. 将工程编译打包为 jar 包。
7. 访问轻舟的可视化管理平台，在`应用`模块下，安装上述应用的 jar 包，至此，已完成应用的开发和部署。
8. 后续，可通过轻舟的可视化管理平台对应用进行管理。

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

【后端】

+ Javassist ([http://www.javassist.org](http://www.javassist.org))
+ Gson ([https://github.com/google/gson](https://github.com/google/gson))
+ Apache MINA SSHD ([https://mina.apache.org/sshd-project/](https://mina.apache.org/sshd-project/))
+ tinylog ([https://tinylog.org](https://tinylog.org))
+ Apache Tomcat ([https://tomcat.apache.org](https://tomcat.apache.org))

## 参与贡献

1. Fork [本仓库](https://gitee.com/openeuler/qingzhou)
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request
