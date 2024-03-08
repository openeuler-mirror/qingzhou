# Qingzhou（轻舟）Web管理软件开发平台

## 平台简介

Qingzhou 是一款设计精巧的轻量级软件开发平台，可用于 Web 管理类型软件的开发，具有开发极速、使用简易、安全可靠、平台兼容等优点。

### 典型用途

Qingzhou 的典型应用场景：

1. 开发一般产品的 Web 管控台
2. 为分布式业务系统做集中式管理

### 主要特性

- 屏蔽视图层，减少开发量：基于 Qingzhou 可以快速开发出一个功能强大的管理后台，而无需对前端页面进行任何编码。
- 声明式 API，编码更高效：基于 Qingzhou 提供的 Java 注解实现业务系统的模块开发，代码简洁，开发效率高。
- 复用通用服务，避免重复造轮：Qingzhou 内置了 Java 序列化、日志、远程 SSH、加密解密等常用功能，通过对应的 API 可轻松使用。
- 插件式应用，灵活管理：可在 Qingzhou 平台上根据业务需要安装或卸载不同的业务系统管理应用。

### 内置功能

- 用户管理：用户是可登录系统的管理员。
- 角色管理：支持角色管理，支持分配权限。
- 部门管理：配置用户的组织机构（公司、部门等）。
- 岗位管理：配置用户所担任职务。
- 通用服务：提供 Java 序列化、日志、远程 SSH、加密解密等常用功能。
- 节点管理：节点是远程管理的基础，用于实现对分布式系统的集中管理。
- 版本管理：支持对平台进行版本管理，可进行版本升级、回退等操作。
- 注册中心：支持将平台服务注册到 Nacos、etcd 等注册中心，实现服务发现和统一配置。
- JMX 接口：支持以 JMX 协议来管理平台内的各类资源。
- 服务监控：监视当前系统 CPU、内存、磁盘、线程、堆栈等相关信息。
- 审计日志：支持审计日志，记录和查询用户的登录及操作信息。
- 菜单及功能定制：控制菜单的层级结构，控制功能的显示和隐藏。
- 功能收藏：用户可以对功能模块进行收藏，方便下次访问。
- 控制台安全
- 启动参数管理
- 日志数据推送
- 监视数据推送

### 运行环境

- JDK >= 1.8

### 项目规划

Qingzhou 的愿景是对各种开源软件和业务系统进行统一的、集中式的、可视化的运维管理，以期为企业的 IT 建设降本增效。
目前计划支持的项目包括 Tomcat、Nacos、OpenSearch、RocketMQ、SpringBootAdmin、Redis 等。

## 安装使用

1. 获取安装包【可选】：在项目根目录执行 `mvn clean package` 命令，之后在 `package/qingzhou/target/qingzhou/qingzhou` 可得到安装包。
2. 启动平台服务：进入安装包根目录，在 bin 目录下，根据操作系统平台执行对应的 start 脚本即开始启动，也可选择执行与平台无关的
   java 命令（`java -jar ./qingzhou-launcher.jar server start`）来启动，看到类似如下的日志输出则表示启动完成：`Open a
   browser to access the Qingzhou console: http://localhost:9000/console`。
3. 访问控制台：平台服务启动完成后，可打开浏览器访问 Qingzhou
   的可视化管理平台：[http://localhost:9000/console](http://localhost:9000/console)。

## 应用开发

基于 Qingzhou 开发应用，只需简单的几步：

1. 在 IDE 中建立一个 Java 项目工程。
2. 在工程里引入 Qingzhou 的依赖 `qingzhou-api` ，并在资源文件夹（如 java 工程的源码目录，或 Maven 工程的 resources
   目录）里创建一个 `qingzhou.properties` 配置文件。
3. 创建一个应用的入口类，并使其继承自 `qingzhou.api.QingZhouApp`（该类在 `qingzhou-api` 内，后文将提到的 Qingzhou
   的类也都在其内），实现 `start`
   方法以定制应用的启动逻辑。`start` 方法会接收一个 `qingzhou.api.AppContext`
   的实例对象，通过该对象，应用可使用平台提供的各种服务。关于服务使用的具体接口可查看对应的
   Javadoc。
4. 将入口类写入 `qingzhou.properties` ，格式如：`qingzhou.app=org.example.MyApp`，其中 `org.example.MyApp` 是应用的入口类的全路径名。
5. 创建应用的模块类，并使其继承自 `qingzhou.api.ModelBase` ，对该类添加 `@Model` 注解以设置模块的名称、图标、菜单、国际化等信息。在该类内部创建
   public 的属性（自动对应到页面上的表单元素），并对其添加 `@ModelField`
   注解以设置属性的相关信息。在该类内部创建方法（自动对应到页面上的按钮或链接），并对其添加 `@ModelAction`
   注解以设置方法的相关信息。关于配置的具体接口，可查看对应的Javadoc。
6. 将工程编译打包为 jar 包。
7. 访问 Qingzhou 的可视化管理平台，在`应用`模块下，安装上述应用的 jar 包，至此即可在平台中对应用进行管理。

## 鸣谢

本项目借鉴和引用了一些优秀项目的设计思路或代码库文件，在此特别感谢原作者的贡献与付出，同时也感谢众多小伙伴们提出项目问题、贡献代码等！

相关项目：

+ Muuri ([https://github.com/haltu/muuri](https://github.com/haltu/muuri))
+ marked ([https://github.com/markedjs/marked](https://github.com/markedjs/marked))
+ Multiple Select ([http://multiple-select.wenzhixin.net.cn](http://multiple-select.wenzhixin.net.cn))
+ Layui layer ([https://gitee.com/layui/layer](https://gitee.com/layui/layer))
+ ZUI ([https://openzui.com](https://openzui.com))
+ jQuery ([https://jquery.com](https://jquery.com))
+ Apache MINA SSHD ([https://mina.apache.org/sshd-project/](https://mina.apache.org/sshd-project/))
+ tinylog ([https://tinylog.org](https://tinylog.org))
+ Apache Tomcat ([https://tomcat.apache.org](https://tomcat.apache.org))

## 参与贡献

1. Fork [本仓库](https://gitee.com/openeuler/qingzhou)
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request
