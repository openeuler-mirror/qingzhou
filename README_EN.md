# Qingzhou

## Overview

Qingzhou is an open-source, lightweight software development platform designed to optimize the quality and efficiency of developing general-purpose web management software, while enabling centralized, unified management across diverse software types.

By developing web management software on Qingzhou, you only need to write simple JavaBeans to automatically generate the corresponding front-end web pages, REST interfaces, JMX interfaces, and internationalization services for your service modules. The platform also provides out-of-the-box built-in capabilities, including user management, authentication and authorization, automated monitoring, file upload/download, local and remote centralized management, and common components.

Within the Qingzhou architecture, the logic of the management service system is encapsulated into customized application packages. These packages can be deployed on demand onto the Qingzhou platform to gain configurability and observability for the service systems, ultimately optimizing the management efficiency of complex enterprise internal systems.

## Environment Requirements

JDK >= 1.8

## Installation and Startup

**Build and Install**

Run the `mvn clean install` command in the project root directory to generate the Qingzhou installation package under `package/target/qingzhou`.
> In this directory:
>
> **bin**: Contains the executable programs for Qingzhou, including script files for different platforms.
>
> **instances**: Stores Qingzhou instances, containing configuration files and application service data.
>
> **lib**: Contains the program files for Qingzhou, organized into directories based on version numbers.

**Start the Service**

There are two ways to start Qingzhou. You can choose either of the following:

- Run the start script for your specific platform in `${Qingzhou_installation_package}/bin`.
- Run the command: `java -jar ${Qingzhou_installation_package}/bin/qingzhou-launcher.jar instance start`

> The service has started successfully if you see log output similar to the following:
>
> Open a browser to access the Qingzhou console: [http://localhost:9001/console](http://localhost:9001/console)

**Access the Console**

Open a browser and navigate to the Qingzhou management platform:
[http://localhost:9001/console](http://localhost:9001/console)

> Notes:
>
> If the browser displays an untrusted IP warning, you can modify the `trustedIp` value in the Qingzhou configuration file `${Qingzhou_installation_package}/instances/default/conf/qingzhou.json`. Specify a regular expression matching the browser IP to trust specific browsers, or set it to `*` to trust all browsers.

## Thanks

This project draws inspiration from and references the design philosophies and codebases of several outstanding projects. We would like to express our sincere gratitude to the original authors for their contributions and dedication, as well as to all the contributors who have reported issues and submitted code.

Related projects:

[Backend]

+ Apache Tomcat ([https://tomcat.apache.org](https://tomcat.apache.org))
+ tinylog ([https://tinylog.org](https://tinylog.org))
+ Gson ([https://github.com/google/gson](https://github.com/google/gson))
+ Javassist ([http://www.javassist.org](http://www.javassist.org))
+ QR-Code-generator ([https://www.nayuki.io/page/qr-code-generator-library](https://www.nayuki.io/page/qr-code-generator-library))
+ Apache MINA SSHD ([https://mina.apache.org/sshd-project](https://mina.apache.org/sshd-project))
+ PlantUML ([https://plantuml.com](https://plantuml.com))
+ SnakeYAML ([https://bitbucket.org/snakeyaml/snakeyaml](https://bitbucket.org/snakeyaml/snakeyaml))

[Frontend]

+ ZUI ([https://openzui.com](https://openzui.com))
+ Apache ECharts ([https://echarts.apache.org](https://echarts.apache.org))
+ jQuery ([https://jquery.com](https://jquery.com))
+ Layui layer ([https://layui.dev](https://layui.dev))
+ Multiple Select ([http://multiple-select.wenzhixin.net.cn](http://multiple-select.wenzhixin.net.cn))
+ Muuri ([https://github.com/haltu/muuri](https://github.com/haltu/muuri))
+ marked ([https://github.com/markedjs/marked](https://github.com/markedjs/marked))
+ Date Range Picker ([https://daterangepicker.com](https://daterangepicker.com))

## Contribution

Contributions make the open-source community a fantastic place to learn, inspire, and innovate. Any contributions you make are greatly appreciated.

If you have any suggestion, please fork this repository and create a pull request (PR). You can also simply open an issue with the tag "enhancement". 
If you find this project helpful, a star would be greatly appreciated!

1. Fork [this repository](https://atomgit.com/openeuler/qingzhou).
2. Create your Feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Create a PR.
