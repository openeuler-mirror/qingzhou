轻舟软件运行时管理平台
========================

平台简介
-------------------

轻舟，提供了一套快速、轻量的开发框架，可用于实现对本地和分布式软件系统的运行时进行集中式、统一化管理。

软件架构
-------------------

![软件架构](doc/img/architecture.jpg)

安装使用
-------------------

* 安装：在项目根目录执行 `mvn clean package` 命令，之后在 `package/qingzhou/target/qingzhou/qingzhou` 可得到安装包。
* 启动：进入安装包根目录，在 bin 目录下根据操作系统平台执行对应的 start 脚本即开始启动，看到类似如下的日志则表示启动完成：`Open a browser to access the QingZhou console: http://localhost:9060/console`。
* 【可选】免脚本启动方式：`java -jar ~/qingzhou/bin/qingzhou-launcher.jar server start`。
* 访问：启动完成后，可打开浏览器访问轻舟的可视化管理平台： [http://localhost:9060/console](http://localhost:9060/console)

免责声明
-------------

本项目基于[MulanPSL-2.0](http://license.coscl.org.cn/MulanPSL2)开源许可协议，代码免费且已开源。使用时请遵循相关开源许可协议!

+ 不得将 qingzhou 用于危害国家安全、荣誉和利益的行为，不能以任何形式用于非法为目的的行为,否则后果自负
+ 虽然本项目在开发过程中很注重安全，但是您仍然需要了解：是软件皆有漏洞，任何人都无法保证软件100%没有漏洞。所以由本软件漏洞造成损失不予赔偿，同时也不承担任何因使用本软件而产生的相关法律责任。也请在软件上线前进行必要的安全监测，避免安全问题发生。

鸣谢
-------------

本项目开发过程中，借鉴和引用了许多优秀项目的设计思路或代码库文件等，在此特别感谢原作者的贡献付出！同时也感谢众多小伙伴提出项目问题及贡献的代码.

主要引用到的项目：

+ tomcat ([https://tomcat.apache.org](https://tomcat.apache.org))

+ tinylog ([https://tinylog.org](https://tinylog.org))

+ apache felix osgi ([https://felix.apache.org](https://felix.apache.org))

+ apache sshd ([https://mina.apache.org/sshd-project/index.html](https://mina.apache.org/sshd-project/index.html))

+ jquery ([https://jquery.com](https://jquery.com))

+ openzui ([https://openzui.com](https://openzui.com))

+ layer ([https://gitee.com/layui/layer](https://gitee.com/layui/layer))

+ marked ([https://github.com/markedjs/marked](https://github.com/markedjs/marked))

+ multiple-select ([http://multiple-select.wenzhixin.net.cn](http://multiple-select.wenzhixin.net.cn))

参与贡献
-------------------

1. Fork [本仓库](https://gitee.com/openeuler/qingzhou)
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request
