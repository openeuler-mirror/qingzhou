# 轻舟Web管理软件开发平台

## 平台简介

轻舟是一个轻量、快速的软件开发平台，可用于Web管理类软件的开发。基于轻舟，开发者可专注于核心业务代码的编写，而无需关心视图层逻辑的实现，从而提高开发效率。轻舟提供了表单页、列表页、监视页、文件下载页等网页显示模板，用以满足不同类型的UI需求，同时内置了与Web管理相关的认证授权、远程部署、REST接口、参数检查等能力，力求为业务系统提供简单、快速且功能完备的开发与管理一体化支撑能力。

### 典型用途

1. 开发一般产品的Web管控台
2. 为分布式软件系统做集中式管理

## 软件架构

### 基本原理

![](doc/readme/basic.png)

### 总体架构

![](doc/readme/architecture.png)

### 轻舟App容器

![](doc/readme/container.png)

## 安装使用

1. 安装：在项目根目录执行 `mvn clean package` 命令，之后在 `package/qingzhou/target/qingzhou/qingzhou` 可得到安装包。
2. 启动：进入安装包根目录，在 bin 目录下，根据操作系统平台执行对应的 start
   脚本即开始启动，看到类似如下的日志则表示启动完成：`Open a browser to access the QingZhou
   console: http://localhost:9000/console`。
3. 【可选】免脚本启动方式：`java -jar ~/qingzhou/bin/qingzhou-launcher.jar server start`。
4. 访问：启动完成后，可打开浏览器访问轻舟的可视化管理平台： [http://localhost:9000/console](http://localhost:9000/console)

## 鸣谢

本项目开发过程中，借鉴和引用了许多优秀项目的设计思路或代码库文件等，在此特别感谢原作者的贡献付出！同时也感谢众多小伙伴提出项目问题及贡献代码.

主要引用到的项目：

+ marked ([https://github.com/markedjs/marked](https://github.com/markedjs/marked))
+ Multiple Select ([http://multiple-select.wenzhixin.net.cn](http://multiple-select.wenzhixin.net.cn))
+ Layui layer ([https://gitee.com/layui/layer](https://gitee.com/layui/layer))
+ ZUI ([https://openzui.com](https://openzui.com))
+ jQuery ([https://jquery.com](https://jquery.com))
+ Apache Tomcat ([https://tomcat.apache.org](https://tomcat.apache.org))
+ Apache MINA SSHD ([https://mina.apache.org/sshd-project/](https://mina.apache.org/sshd-project/))
+ tinylog ([https://tinylog.org](https://tinylog.org))
+ Apache Felix ([https://felix.apache.org](https://felix.apache.org))

## 参与贡献

1. Fork [本仓库](https://gitee.com/openeuler/qingzhou)
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request
