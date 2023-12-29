package qingzhou.console.master.support;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.Model;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.model.DownloadModel;
import qingzhou.api.console.model.EditModel;
import qingzhou.api.console.model.ListModel;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.master.MasterModelBase;
import qingzhou.console.util.Constants;
import qingzhou.console.util.FileUtil;
import qingzhou.console.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Model(name = "support", icon = "check",
        menuName = "Support", menuOrder = 1,
        nameI18n = {"适配清单", "en:Support List"},
        infoI18n = {"列出轻舟平台适配的产品、服务器、厂商等清单。",
                "en:List the products, servers, manufacturers, etc. that are suitable for the Qingzhou platform."})
public class SupportList extends MasterModelBase implements ListModel, EditModel, DownloadModel {
    private static final String usagePrefix = "Support.usage.";
    private static final String summaryPrefix = "Support.summary.";
    private static final String pinpoint = "Pinpoint";
    private static final String snmp = "SNMP";
    private static final String prometheus = "Prometheus";
    private static final String skyWalking = "SkyWalking";
    private static final String zabbix = "Zabbix";
    private static final String redis = "Redis";
    private static final String Elasticsearch = "Elasticsearch";
    //    private static final String ncs = RegistryVendor.TongNCS.name(); //todo
    private static final String ncs = "ncs";//todo
    private static final String nacos = "Nacos";
    private static final String zk = "ZooKeeper";
    private static final String ectd = "Ectd";

    static {
        ConsoleContext master = ConsoleWarHelper.getMasterAppConsoleContext();
        if (master != null) {
            master.addI18N("validator.action.notsupport", new String[]{"没有可下载的资源", "en:There are no downloadable resources"});
            master.addI18N(SupportList.summaryPrefix + snmp, new String[]{"通过 SNMP 管理端获取 TongWeb 的监控数据。", "en:Obtain TongWeb monitoring data through SNMP management terminal."});
            master.addI18N(SupportList.usagePrefix + snmp, new String[]{"在控制台中开启“SNMP 服务”，填写 IP、端口、身份认证、加密密码即可；\n" +
                    "支持 GET、GETBULK 调用，以 json 格式返回数据。\n" +
                    "OID 与监控数据对应关系： \n" +
                    "1.3.6.1.4.1.55566.1.0: 操作系统信息；\n" +
                    "1.3.6.1.4.1.55566.1.1: Java虚拟机信息；\n" +
                    "1.3.6.1.4.1.55566.\n" +
                    "1.2: Java类加载信息；\n" +
                    "1.3.6.1.4.1.55566.1.3: Java编译信息；\n" +
                    "1.3.6.1.4.1.55566.1.4: Java线程信息；\n" +
                    "1.3.6.1.4.1.55566.1.5: 连接池信息；\n" +
                    "1.3.6.1.4.1.55566.1.6: 线程池信息；\n" +
                    "1.3.6.1.4.1.55566.1.7: 应用线程信息(线程个数、的状态)；\n" +
                    "1.3.6.1.4.1.55566.1.8: 服务信息(包含授权信息)。",
                    "en:Enable the SNMP service in the console and fill in the IP, port, authentication, and encryption password; supports GET, GETBULK calls and returns data in json format.\n" +
                            "The corresponding relationship between OID and monitoring data,\n" +
                            "1.3.6.1.4.1.55566.1.0: Operating system information;\n" +
                            "1.3.6.1.4.1.55566.1.1: Java virtual machine information;\n" +
                            "1.3.6.1.4.1.55566.1.2: Java class loading information;\n" +
                            "1.3.6.1.4.1. 55566.1.3: Java compilation information;\n" +
                            "1.3.6.1.4.1.55566.1.4: Java thread information;\n" +
                            "1.3.6.1.4.1.55566.1.5: Connection pool information;\n" +
                            "1.3.6.1.4.1.55566.1.6: Thread pool information;\n" +
                            "1.3.6.1.4.1.55566.1.7: Application thread information (number of threads, status);\n" +
                            "1.3.6.1.4.1.55566.1.8: Service information (including authorization information)."});

            master.addI18N(SupportList.summaryPrefix + prometheus, new String[]{"通过 Prometheus 管理端获取 TongWeb 的监控数据。", "en:Obtain TongWeb monitoring data through Prometheus management terminal."});
            master.addI18N(SupportList.usagePrefix + prometheus, new String[]{"在控制台中开启 Prometheus 服务，根据数据上报模式填写 IP 地址和端口信息或(和)时间间隔即可。\n" +
                    "\n【拉取】模式\n" +
                    "开启此功能后，输入本机 IP 地址和端口号，将开启一个 Web 服务，\n" +
                    "用于接收（Prometheus）请求，响应监控数据到 Prometheus。\n" +
                    "\n【推送】模式\n" +
                    "开启此功能后，输入 pushGateway 服务地址，并指定推送时间间隔（秒），\n" +
                    "内部将启动一个定时任务定时将监控数据推送到 pushGateway，\n" +
                    "Prometheus 将从 pushGateway 拉取监控数据。",
                    "en:Open the Prometheus service in the console and simply fill in the ip address and port information or (and) the time interval, depending on the data reporting mode.\n" +
                            "[Pull Mode]\n" +
                            "When this feature is turned on and the local IP address and port number are entered, \n" +
                            "a web service will be opened for receiving (Prometheus) requests and responding to monitoring data to Prometheus.\n" +
                            "[Push Mode]\n" +
                            "When this feature is enabled, enter the pushGateway service address and specify the push interval (in seconds), \n" +
                            "a timed task will be started internally to push monitoring data to pushGateway at regular intervals, \n" +
                            "and Prometheus will pull monitoring data from pushGateway."});

            master.addI18N(SupportList.summaryPrefix + pinpoint, new String[]{"通过 pinpoint 监控 TongWeb 的 Servlet 调用、类加载、内存使用、线程使用以及调用时间等。", "en:Monitor TongWeb servlet invocation, class loading, memory usage, thread usage, and invocation time through pinpoint."});
            master.addI18N(SupportList.usagePrefix + pinpoint, new String[]{"首先确认 pinpoint 为 2.4.0 版本。\n" +
                    "第一步: 将 tongweb-pinpoint.jar 上传到 pinpoint-agent 的插件目录；\n" +
                    "第二步: 修改 pinpoint-agent 的 profiles 目录下的 release 或者 local 中的 pinpoint.config，添加如下参数：\n" +
                    "profiler.tongweb.enable=true \n" +
                    "profiler.tongweb.bootstrap.main=com.tongweb.main.TongWebMain \n" +
                    "profiler.tongweb.hidepinpointheader=true \n" +
                    "profiler.tongweb.excludeurl=profiler.tongweb.tracerequestparam=true \n" +
                    "第三步: 配置启动参数，配置完成后需要重启 TongWeb，配置方式有两种：\n" +
                    "1. 修改 TongWeb 的 tongweb.xml，添加 \n" +
                    "<arg name=\"-javaagent:/opt/pinpoint-agent-2.4.0/pinpoint-bootstrap-2.4.0.jar\"/>\n" +
                    "<arg name=\"-Dpinpoint.agentId=TongWeb-pinpoint-agentId\"/>\n" +
                    "<arg name=\"-Dpinpoint.applicationName=applicationName\"/>\n" +
                    "2. 登录控制台 > 基础配置 > 启动参数，添加\n" +
                    "-javaagent:/opt/pinpoint-agent-2.4.0/pinpoint-bootstrap-2.4.0.jar\n" +
                    "-Dpinpoint.agentId=TongWeb-pinpoint-agentId\n" +
                    "-Dpinpoint.applicationName=applicationName\n" +
                    "注意: -Dpinpoint.applicationName的值只能是[a-zA-Z0-9]，“.”，“-”，“_”组成并且长度不能大于24个字符。\n",
                    "en:First confirm that pinpoint is version 2.4.0.\n" +
                            "Step 1: Upload tongweb-pinpoint.jar to the plugin directory of pinpoint-agent;\n" +
                            "Step 2: Modify pinpoint.config in release or local in the profiles directory of pinpoint-agent, add profiler.tongweb.enable=true, profiler.tongweb.bootstrap.main=com.tongweb.main.TongWebMain, profiler.tongweb.hidepinpointheader=true, profiler.tongweb.excludeurl=profiler.tongweb.tracerequestparam=true;\n" +
                            "Step 3: Configure startup parameters. After the configuration, tongweb needs to be restarted. There are two configuration methods: \n" +
                            "1: Modify tongweb.xml of tongweb, add \n" +
                            "<arg name=\"-javaagent:/opt/pinpoint-agent-2.4.0/pinpoint-bootstrap-2.4.0.jar\"/>\n" +
                            "<arg name=\"-Dpinpoint.agentId=TongWeb-pinpoint-agentId\"/>\n" +
                            "<arg name=\"-Dpinpoint.applicationName=applicationName\"/>\n" +
                            "2:Login to Console -> Basic Configuration -> Startup Parameters and add\n" +
                            "-javaagent:/opt/pinpoint-agent-2.4.0/pinpoint-bootstrap-2.4.0.jar\n" +
                            "-Dpinpoint.agentId=TongWeb-pinpoint-agentId\n" +
                            "-Dpinpoint.applicationName=applicationName\n" +
                            "Note: The value of -Dpinpoint.applicationName can only be [a-zA-Z0-9], \".\", \"-\", \"_\" and the length cannot be greater than 24 characters."});
            master.addI18N(SupportList.summaryPrefix + skyWalking, new String[]{"SkyWalking 是分布式系统的应用程序性能监视工具，包括了分布式追踪，性能指标分析和服务依赖分析等。", "en:SkyWalking is an application performance monitoring tool for distributed systems, including distributed tracking, performance index analysis and service dependency analysis."});
            master.addI18N(SupportList.usagePrefix + skyWalking, new String[]{"第一步: 官网分别下载 apache-skywalking-apm-9.3.0.tar.gz 和 apache-skywalking-java-agent-8.14.0.tgz，并解压放到 skyWalking 文件下；\n" +
                    "下载地址：\n" +
                    "https://www.apache.org/dyn/closer.cgi/skywalking/9.3.0/apache-skywalking-apm-9.3.0.tar.gz\n" +
                    "https://www.apache.org/dyn/closer.cgi/skywalking/java-agent/8.14.0/apache-skywalking-java-agent-8.14.0.tgz\n" +
                    "第二步: 将 tongweb-skywalking.jar 放到 skywalking\\skywalking-agent\\plugins 目录下。\n" +
                    "第三步: 配置启动参数，配置完成后需要重启 TongWeb，配置方式有两种： \n" +
                    "1. 修改 TongWeb 的 tongweb.xml，添加 \n" +
                    "<arg name=\"-javaagent:D:/skywalking/skywalking-agent/skywalking-agent.jar \"/>\n" +
                    "2. 登录控制台 > 基础配置 > 启动参数，添加 \n" +
                    "-javaagent:D:/skywalking/skywalking-agent/skywalking-agent.jar \n" +
                    "第四步: 启动本地 h2，接着修改 skywalking\\apache-skywalking-apm-bin\\config\\application.yml 中的 h2 的用户名密码以及 jdbcUrl，如下所示； \n" +
                    " h2:\n" +
                    "properties:\n" +
                    "jdbcUrl: ${SW_STORAGE_H2_URL:h2数据库的jdbcUrl;DB_CLOSE_DELAY=-1}\n" +
                    "dataSource.user: ${SW_STORAGE_H2_USER:用户名}\n" +
                    "第五步: 修改服务名称，在 skywalking-agent\\config\\agent.config,修改 Your_ApplicationName 为 TongWeb；\n" +
                    "修改组件名称，在 skywalking\\apache-skywalking-apm-bin\\config\\component-libraries.yml 中，添加 \n" +
                    "TongWeb:\n" +
                    "id: 141\n" +
                    "languages: Java\n" +
                    " 注意格式，要跟 component-libraries.yml 中其它配置格式保持一致。\n" +
                    "第六步: 在 skywalking\\apache-skywalking-apm-bin\\webapp\\application.yml 中 serverPort: ${SW_SERVER_PORT:-8080}，修改 8080 为 9010（避免 8080 端口冲突）。\n" +
                    "启动 SkyWalking: 分别执行 skywalking\\apache-skywalking-apm-bin\\ 下的 oapService.bat 和 webappService.bat，启动成功后访问: http://localhost:9010。\n" +
                    "第七步: TongWeb 部署任意应用，并访问，查看 SkyWalking 控制台，点击“普通服务” > “服务”，进入列表页；点击表格中的服务器名称“Trace”，会看到刚刚访问部署在 TongWeb 上的应用访问日志，代表 SkyWalking 在 TongWeb 适配成功。",
                    "en:Step 1: download apache-skywalking-apm-9.3.0.tar.gz and apache-skywalking-java-agent-8.14.0.tgz on the official website, and extract them into the skywalking file.\n" +
                            "Download address:\n" +
                            "https://www.apache.org/dyn/closer.cgi/skywalking/9.3.0/apache-skywalking-apm-9.3.0.tar.gz\n" +
                            "https://www.apache.org/dyn/closer.cgi/skywalking/java-agent/8.14.0/apache-skywalking-java-agent-8.14.0.tgz\n" +
                            "Step 2: Put tongweb-skywalking.jar in the directory of skywalking skywalking-agent plugins.\n" +
                            "Step 3: Configure startup parameters. After the configuration, tongweb needs to be restarted. There are two configuration methods\n" +
                            "1: Modify tongweb.xml for tongweb, add \n" +
                            "<arg name=\"-javaagent:D:/skywalking/skywalking-agent/skywalking-agent.jar \"/>\n" +
                            "2: Log in to Console -> Basic Configuration -> Startup Parameters and add\n" +
                            "-javaagent:D:/skywalking/skywalking-agent/skywalking-agent.jar \n" +
                            "Step 4: Start local h2, and then modify the username and password of h2 and jdbcUrl in skywalking apache-skywalking-apm-bin config application.yml, as follows:\n" +
                            "h2:\n" +
                            "properties:\n" +
                            "jdbcUrl: ${SW_STORAGE_H2_URL:h2 database jdbcUrl;DB_CLOSE_DELAY=-1}\n" +
                            "dataSource.user: ${SW_STORAGE_H2_USER:username}\n" +
                            "Step 5: Modify the service name. In skywalking-agent config agent.config, modify Your_ApplicationName to TongWeb.\n" +
                            "Modify the component name, in skywalking apache-skywalking-apm-bin config component-libraries.yml, add:\n" +
                            "TongWeb:\n" +
                            "id: 141\n" +
                            "languages: Java\n" +
                            "Note that the format should be consistent with other configuration formats in component-libraries.yml.\n" +
                            "Step 6: serverPort in skywalking\\ apache-skywalk-apm-bin \\webapp\\application.yml ${SW_SERVER_PORT:-8080} to change 8080 to 9010 (to avoid port conflicts).\n" +
                            "Start skywalking: execute oapService.bat and webappService.bat under skywalking apache-skywalking-apm-bin respectively, and access them after successful startup: http://localhost:9010 .\n" +
                            "Step 7: deploy any application on tongweb and visit it. View the skywalking console, click \"General Service\" ->\"Service\", enter the list page, click the server name in the table ->\"Trace\", and you will see the application access log just accessed and deployed on tongweb, indicating that skywalking has successfully adapted on tongweb."});

            master.addI18N(SupportList.summaryPrefix + zabbix, new String[]{"Zabbix 可以基于 TongWeb 提供的监视接口进行资源监视、预警。",
                    "en:Zabbix can perform resource monitoring and alerting based on the monitoring interface provided by TongWeb."});
            master.addI18N(SupportList.usagePrefix + zabbix, new String[]{
                    "【TongWeb 配置】： \n" +
                            "1. 将 zabbix-server 所在服务器的 IP 添加至 TongWeb 控制台的信任 IP 列表（通过控制台“安全配置” > “控制台安全”或者修改配置文件 console.xml）；\n" +
                            "2. 关闭控制台 CSRF 防护功能（修改 ${tongweb.base}/conf/tongweb.xml，将 console 应用的“csrfPrevention”参数设置为“false”）。\n" +
                            "【Zabbix 配置】： \n" +
                            "1. 下载 TongWeb 监视模板；\n" +
                            "2. 将模板导入至 zabbix 的模板中；\n" +
                            "3. 创建 TongWeb 主机，选择模板“Template TongWeb by HTTP”（默认在分组 Templates 中），并且修改宏配置中的 TongWeb 的 IP、Port、用户名、密码为需要监视的 TongWeb。\n" +
                            "\n" +
                            "注：TongWeb 的监视模板中只包含部分 TongWeb 监视项，且未设置触发器，如需要请自行扩展。", "en:TongWeb configuration: 1.\n" +
                    "1. Add the IP of the server where zabbix-server is located to the trusted IP list of TongWeb console (via console \"Security Configuration - Console Security\" or modify the configuration file console.xml).\n" +
                    "2. Turn off the console CSRF protection (modify ${tongweb.base}/conf/tongweb.xml and set the \"csrfPrevention\" parameter of the console application to \"false \").\n" +
                    "Zabbix configuration: 1.\n" +
                    "1. Download the TongWeb monitoring template.\n" +
                    "2. Import the template into the zabbix template.\n" +
                    "3. Create a TongWeb host, select the template \"Template TongWeb by HTTP\" (default in the group Templates), and modify the ip, port, username, and password of TongWeb in the macro configuration for the TongWeb to be monitored.\n" +
                    "\n" +
                    "Note: The monitoring template of TongWeb only contains part of TongWeb monitoring items, and no trigger is set, please extend it by yourself if needed.\n"});
            master.addI18N(SupportList.summaryPrefix + redis, new String[]{"将应用的 Session 存储到外部 Redis 服务器，以获得会话高可用能力。",
                    "en:Store your app Session to an external Redis server for session availability."});
            master.addI18N(SupportList.usagePrefix + redis, new String[]{
                    "使用方法： \n" +
                            "第一步：请通过官方渠道下载如下 jar 包，并放入 ${tongweb.base}/lib 目录下；\n" +
                            "lettuce-core-6.2.3.RELEASE.jar \n" +
                            "netty-buffer-4.1.89.Final.jar \n" +
                            "netty-codec-4.1.89.Final.jar \n" +
                            "netty-common-4.1.89.Final.jar \n" +
                            "netty-handler-4.1.89.Final.jar \n" +
                            "netty-resolver-4.1.89.Final.jar \n" +
                            "netty-transport-4.1.89.Final.jar \n" +
                            "reactive-streams-1.0.4.jar \n" +
                            "reactor-core-3.4.27.jar \n" +
                            "第二步：通过 TongWeb 控制台或其它方式，建立“外部会话服务器”资源，输入相应的 Redis 服务器连接参数；\n" +
                            "第三步：通过 TongWeb 控制台或其它方式，部署应用，打开“使用外部会话服务器”，并选择上一步建立的“外部会话服务器”资源即可。",
                    "en:How to use:\n" +
                            "1. Please download lettuce-core-6.2.3.RELEASE.jar, \n" +
                            "netty-buffer-4.1.89.Final.jar, netty-codec-4.1.89.Final.jar, netty-common-4.1.89.Final.jar, \n" +
                            "netty-handler-4.1.89.Final.jar, netty-resolver-4.1.89.Final.jar, netty-transport-4.1.89.Final.jar, \n" +
                            "reactive-streams-1.0.4.jar, reactor-core-3.4.27.jar, \n" +
                            "put in the ${tongweb.base}/lib directory;\n" +
                            "2. Create an \"external session server\" resource through the TongWeb console or other methods, and enter the corresponding Redis server connection parameters;\n" +
                            "3. Deploy the application through the TongWeb console or other methods, open Use External Session Server, and select the External Session Server resource created in the previous step."});
            master.addI18N(SupportList.summaryPrefix + Elasticsearch,
                    new String[]{
                            "将 TongWeb 的运行日志推送到 Elasticsearch。",
                            "en:Push TongWeb run logs to Elasticsearch."});
            master.addI18N(SupportList.usagePrefix + Elasticsearch, new String[]{
                    "使用方法：\n" +
                            "请在“日志推送”模块查看其使用说明。",
                    "en:How to use:\n" +
                            "Please see the instructions for using it in the Log Push module."});
            master.addI18N(SupportList.summaryPrefix + nacos,
                    new String[]{"Nacos 是构建云原生应用的动态服务发现、配置管理和服务管理平台。",
                            "en:Nacos is a dynamic service discovery, configuration management, and service management platform for building cloud-native applications."});
            master.addI18N(SupportList.usagePrefix + nacos,
                    new String[]{
                            "使用方法: \n" +
                                    "1. 通过 Nacos  官方渠道下载 2.0 以上版本的 Nacos 客户端 jar 包以及所有的依赖，以下是 2.2.3 版本 jar 包清单\n" +
                                    "commons-codec-1.15.jar \n" +
                                    "commons-logging-1.2.jar \n" +
                                    "httpasyncclient-4.1.3.jar \n" +
                                    "httpclient-4.5.3.jar \n" +
                                    "httpcore-4.4.6.jar \n" +
                                    "httpcore-nio-4.4.6.jar \n" +
                                    "jackson-annotations-2.12.7.jar \n" +
                                    "jackson-core-2.12.6.jar \n" +
                                    "jackson-databind-2.12.7.1.jar \n" +
                                    "nacos-auth-plugin-2.2.3.jar \n" +
                                    "nacos-client-2.2.3.jar \n" +
                                    "nacos-encryption-plugin-2.2.3.jar \n" +
                                    "simpleclient_tracer_common-0.12.0.jar \n" +
                                    "simpleclient_tracer_otel_agent-0.12.0.jar \n" +
                                    "simpleclient_tracer_otel-0.12.0.jar \n" +
                                    "simpleclient-0.12.0.jar \n" +
                                    "slf4j-api-2.0.7.jar \n" +
                                    "snakeyaml-2.jar \n" +
                                    "2. 将第一步中的 jar 包放到 ${tongweb.home}/lib 或 ${tongweb.base}/lib 目录下；\n" +
                                    "3. 启动 TongWeb，访问控制台，在服务注册中填写连接信息。注：需要保持“全局配置”的“支持集中管理”为开启状态。",
                            "en:How to use:\n" +
                                    "1.  Download the Nacos client jar package and all dependencies from version 2.0 or above through the official Nacos channel, and the following is the list of 2.2.3 jar packages \n" +
                                    "commons-codec-1.15.jar \n" +
                                    "commons-logging-1.2.jar \n" +
                                    "httpasyncclient-4.1.3.jar \n" +
                                    "httpclient-4.5.3.jar \n" +
                                    "httpcore-4.4.6.jar \n" +
                                    "httpcore-nio-4.4.6.jar \n" +
                                    "jackson-annotations-2.12.7.jar \n" +
                                    "jackson-core-2.12.6.jar \n" +
                                    "jackson-databind-2.12.7.1.jar \n" +
                                    "nacos-auth-plugin-2.2.3.jar \n" +
                                    "nacos-client-2.2.3.jar \n" +
                                    "nacos-encryption-plugin-2.2.3.jar \n" +
                                    "simpleclient_tracer_common-0.12.0.jar \n" +
                                    "simpleclient_tracer_otel_agent-0.12.0.jar \n" +
                                    "simpleclient_tracer_otel-0.12.0.jar \n" +
                                    "simpleclient-0.12.0.jar \n" +
                                    "slf4j-api-2.0.7.jar \n" +
                                    "snakeyaml-2.jar \n" +
                                    "2. Put the jar package in the first step in the ${tongweb.home}/lib or ${tongweb.base}/lib directory; \n" +
                                    "3. Start TongWeb, access the console, and enter the connection information in the service registration. Note: You need to keep \"Support centralized management\" in \"Global Configuration\" turned on."});

            master.addI18N(SupportList.summaryPrefix + ncs,
                    new String[]{"TongNCS 是构建云原生应用的动态服务发现、配置管理和服务管理平台。",
                            "en:TongNCS is a dynamic service discovery, configuration management, and service management platform for building cloud-native applications."});
            master.addI18N(SupportList.usagePrefix + ncs,
                    new String[]{
                            "使用方法: \n" +
                                    "1. 通过东方通获取 TongNCS 的 java 客户端 jar 包以及所有的依赖包，以下是 2.0.1 版本 jar 包清单 \n" +
                                    "commons-codec-1.15.jar \n" +
                                    "commons-logging-1.2.jar \n" +
                                    "httpasyncclient-4.1.3.jar \n" +
                                    "httpclient-4.5.3.jar \n" +
                                    "httpcore-4.4.6.jar \n" +
                                    "httpcore-nio-4.4.6.jar \n" +
                                    "jackson-annotations-2.12.7.jar \n" +
                                    "jackson-core-2.12.6.jar \n" +
                                    "jackson-databind-2.12.7.1.jar \n" +
                                    "logback-classic-1.2.3.jar \n" +
                                    "logback-core-1.2.3.jar \n" +
                                    "simpleclient_tracer_common-0.12.0.jar \n" +
                                    "simpleclient_tracer_otel_agent-0.12.0.jar \n" +
                                    "simpleclient_tracer_otel-0.12.0.jar \n" +
                                    "simpleclient-0.12.0.jar \n" +
                                    "slf4j-api-1.7.25.jar \n" +
                                    "snakeyaml-2.0.jar \n" +
                                    "tongncs-auth-plugin-2.0.1.jar \n" +
                                    "tongncs-client-2.0.1.jar \n" +
                                    "tongncs-encryption-plugin-2.0.1.jar \n" +
                                    "2. 将第一步中的 jar 包放到 ${tongweb.home}/lib 或 ${tongweb.base}/lib 目录下；\n" +
                                    "3. 启动 TongWeb，访问控制台，在服务注册中填写连接信息。注：需要保持“全局配置”的“支持集中管理”为开启状态。",
                            "en:How to use:\n" +
                                    "1.  To obtain the TongNCS java client jar package and all dependent packages through Dongfangtong, the following is the list of jar packages in version 2.0.1 \n" +
                                    "commons-codec-1.15.jar \n" +
                                    "commons-logging-1.2.jar \n" +
                                    "httpasyncclient-4.1.3.jar \n" +
                                    "httpclient-4.5.3.jar \n" +
                                    "httpcore-4.4.6.jar \n" +
                                    "httpcore-nio-4.4.6.jar \n" +
                                    "jackson-annotations-2.12.7.jar \n" +
                                    "jackson-core-2.12.6.jar \n" +
                                    "jackson-databind-2.12.7.1.jar \n" +
                                    "logback-classic-1.2.3.jar \n" +
                                    "logback-core-1.2.3.jar \n" +
                                    "simpleclient_tracer_common-0.12.0.jar \n" +
                                    "simpleclient_tracer_otel_agent-0.12.0.jar \n" +
                                    "simpleclient_tracer_otel-0.12.0.jar \n" +
                                    "simpleclient-0.12.0.jar \n" +
                                    "slf4j-api-1.7.25.jar \n" +
                                    "snakeyaml-2.0.jar \n" +
                                    "tongncs-auth-plugin-2.0.1.jar \n" +
                                    "tongncs-client-2.0.1.jar \n" +
                                    "tongncs-encryption-plugin-2.0.1.jar \n" +
                                    "2. Put the jar package in the first step in the ${tongweb.home}/lib or ${tongweb.base}/lib directory; \n" +
                                    "3. Start TongWeb, access the console, and enter the connection information in the service registration. Note: You need to keep \"Support centralized management\" in \"Global Configuration\" turned on."});

            master.addI18N(SupportList.summaryPrefix + zk, new String[]{
                    "Zookeeper 是一个分布式的、开源的程序协调服务。",
                    "en:Zookeeper is a distributed, open source program coordination service."});
            master.addI18N(SupportList.usagePrefix + zk,
                    new String[]{
                            "使用方法: \n" +
                                    "1. 支持 ZooKeeper 3.5 及以上的版本，根据 ZooKeeper 版本下载对应的 apache curator 版本的 jar 包以及依赖包，以下是 curator5.5.0 版本 jar 包清单 \n" +
                                    "audience-annotations-0.12.0.jar \n" +
                                    "checker-qual-3.12.0.jar \n" +
                                    "curator-client-5.5.0.jar \n" +
                                    "curator-framework-5.5.0.jar \n" +
                                    "curator-recipes-5.5.0.jar \n" +
                                    "curator-x-discovery-5.5.0.jar \n" +
                                    "error_prone_annotations-2.11.0.jar \n" +
                                    "failureaccess-1.0.1.jar \n" +
                                    "guava-31.1-jre.jar \n" +
                                    "j2objc-annotations-1.3.jar \n" +
                                    "jackson-annotations-2.10.0.jar \n" +
                                    "jackson-core-2.10.0.jar \n" +
                                    "jackson-databind-2.10.0.jar \n" +
                                    "jsr305-3.0.2.jar \n" +
                                    "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar \n" +
                                    "netty-buffer-4.1.76.Final.jar \n" +
                                    "netty-codec-4.1.76.Final.jar \n" +
                                    "netty-common-4.1.76.Final.jar \n" +
                                    "netty-handler-4.1.76.Final.jar \n" +
                                    "netty-resolver-4.1.76.Final.jar \n" +
                                    "netty-transport-4.1.76.Final.jar \n" +
                                    "netty-transport-classes-epoll-4.1.76.Final.jar \n" +
                                    "netty-transport-native-epoll-4.1.76.Final.jar \n" +
                                    "netty-transport-native-unix-common-4.1.76.Final.jar \n" +
                                    "slf4j-api-1.7.25.jar \n" +
                                    "zookeeper-3.7.1.jar \n" +
                                    "zookeeper-jute-3.7.1.jar \n" +
                                    "2. 将第一步中的 jar 包放到 ${tongweb.home}/lib 或 ${tongweb.base}/lib 目录下；\n" +
                                    "3. 启动 TongWeb，访问控制台，在服务注册中填写连接信息。注：需要保持“全局配置”的“支持集中管理”为开启状态。",
                            "en:How to use:\n" +
                                    "1.  Support ZooKeeper 3.5 and above, download the corresponding Apache Curator version jar package and dependent packages according to the ZooKeeper version, the following is the list of Curator 5.5.0 jar package \n" +
                                    "audience-annotations-0.12.0.jar \n" +
                                    "checker-qual-3.12.0.jar \n" +
                                    "curator-client-5.5.0.jar \n" +
                                    "curator-framework-5.5.0.jar \n" +
                                    "curator-recipes-5.5.0.jar \n" +
                                    "curator-x-discovery-5.5.0.jar \n" +
                                    "error_prone_annotations-2.11.0.jar \n" +
                                    "failureaccess-1.0.1.jar \n" +
                                    "guava-31.1-jre.jar \n" +
                                    "j2objc-annotations-1.3.jar \n" +
                                    "jackson-annotations-2.10.0.jar \n" +
                                    "jackson-core-2.10.0.jar \n" +
                                    "jackson-databind-2.10.0.jar \n" +
                                    "jsr305-3.0.2.jar \n" +
                                    "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar \n" +
                                    "netty-buffer-4.1.76.Final.jar \n" +
                                    "netty-codec-4.1.76.Final.jar \n" +
                                    "netty-common-4.1.76.Final.jar \n" +
                                    "netty-handler-4.1.76.Final.jar \n" +
                                    "netty-resolver-4.1.76.Final.jar \n" +
                                    "netty-transport-4.1.76.Final.jar \n" +
                                    "netty-transport-classes-epoll-4.1.76.Final.jar \n" +
                                    "netty-transport-native-epoll-4.1.76.Final.jar \n" +
                                    "netty-transport-native-unix-common-4.1.76.Final.jar \n" +
                                    "slf4j-api-1.7.25.jar \n" +
                                    "zookeeper-3.7.1.jar \n" +
                                    "zookeeper-jute-3.7.1.jar \n" +
                                    "2. Put the jar package in the first step in the ${tongweb.home}/lib or ${tongweb.base}/lib directory; \n" +
                                    "3. Start TongWeb, access the console, and enter the connection information in the service registration. Note: You need to keep \"Support centralized management\" in \"Global Configuration\" turned on."});

            master.addI18N(SupportList.summaryPrefix + ectd, new String[]{
                    "etcd 是用于共享配置和服务发现的分布式、一致性的 KV 存储系统。",
                    "en:etcd is a distributed, consistent KV storage system for shared configuration and service discovery."});
            master.addI18N(SupportList.usagePrefix + ectd,
                    new String[]{
                            "使用方法: \n" +
                                    "1. 支持 etcd v3 版本，请通过 etcd 官方渠道下载 java 客户端 jetcd 的 jar 包以及所有的依赖，以下是 jetcd 0.7.6 版本 jar 包清单 \n" +
                                    "checker-qual-3.33.0.jar \n" +
                                    "failsafe-2.4.4.jar \n" +
                                    "failureaccess-1.0.1.jar \n" +
                                    "grpc-api-1.56.1.jar \n" +
                                    "grpc-context-1.56.1.jar \n" +
                                    "grpc-core-1.56.1.jar \n" +
                                    "grpc-grpclb-1.56.1.jar \n" +
                                    "grpc-netty-1.56.1.jar \n" +
                                    "grpc-protobuf-1.56.1.jar \n" +
                                    "grpc-protobuf-lite-1.56.1.jar \n" +
                                    "grpc-stub-1.56.1.jar \n" +
                                    "guava-32.1.1-jre.jar \n" +
                                    "jackson-core-2.15.0.jar \n" +
                                    "jetcd-api-0.7.6.jar \n" +
                                    "jetcd-common-0.7.6.jar \n" +
                                    "jetcd-core-0.7.6.jar \n" +
                                    "jetcd-grpc-0.7.6.jar \n" +
                                    "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar \n" +
                                    "netty-buffer-4.1.94.Final.jar \n" +
                                    "netty-codec-4.1.87.Final.jar \n" +
                                    "netty-codec-dns-4.1.94.Final.jar \n" +
                                    "netty-codec-http2-4.1.87.Final.jar \n" +
                                    "netty-codec-http-4.1.94.Final.jar \n" +
                                    "netty-codec-socks-4.1.87.Final.jar \n" +
                                    "netty-common-4.1.94.Final.jar \n" +
                                    "netty-handler-4.1.94.Final.jar \n" +
                                    "netty-handler-proxy-4.1.87.Final.jar \n" +
                                    "netty-resolver-4.1.94.Final.jar \n" +
                                    "netty-resolver-dns-4.1.94.Final.jar \n" +
                                    "netty-transport-4.1.94.Final.jar \n" +
                                    "netty-transport-native-unix-common-4.1.87.Final.jar \n" +
                                    "perfmark-api-0.26.0.jar \n" +
                                    "protobuf-java-3.22.3.jar \n" +
                                    "protobuf-java-util-3.22.3.jar \n" +
                                    "proto-google-common-protos-2.17.0.jar \n" +
                                    "slf4j-api-2.0.7.jar \n" +
                                    "vertx-core-4.4.4.jar \n" +
                                    "vertx-grpc-4.4.4.jar \n" +
                                    "2. 将第一步中的 jar 包放到 ${tongweb.home}/lib 或 ${tongweb.base}/lib 目录下；\n" +
                                    "3. 启动 TongWeb，访问控制台，在服务注册中填写连接信息。注：需要保持“全局配置”的“支持集中管理”为开启状态。",
                            "en:How to use:\n" +
                                    "1.   To support etcd v3 version, please download the jar package of the java client jetcd and all dependencies through the official channel of ectd, the following is the jetcd version 0.7.6 jar package list \n" +
                                    "checker-qual-3.33.0.jar \n" +
                                    "failsafe-2.4.4.jar \n" +
                                    "failureaccess-1.0.1.jar \n" +
                                    "grpc-api-1.56.1.jar \n" +
                                    "grpc-context-1.56.1.jar \n" +
                                    "grpc-core-1.56.1.jar \n" +
                                    "grpc-grpclb-1.56.1.jar \n" +
                                    "grpc-netty-1.56.1.jar \n" +
                                    "grpc-protobuf-1.56.1.jar \n" +
                                    "grpc-protobuf-lite-1.56.1.jar \n" +
                                    "grpc-stub-1.56.1.jar \n" +
                                    "guava-32.1.1-jre.jar \n" +
                                    "jackson-core-2.15.0.jar \n" +
                                    "jetcd-api-0.7.6.jar \n" +
                                    "jetcd-common-0.7.6.jar \n" +
                                    "jetcd-core-0.7.6.jar \n" +
                                    "jetcd-grpc-0.7.6.jar \n" +
                                    "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar \n" +
                                    "netty-buffer-4.1.94.Final.jar \n" +
                                    "netty-codec-4.1.87.Final.jar \n" +
                                    "netty-codec-dns-4.1.94.Final.jar \n" +
                                    "netty-codec-http2-4.1.87.Final.jar \n" +
                                    "netty-codec-http-4.1.94.Final.jar \n" +
                                    "netty-codec-socks-4.1.87.Final.jar \n" +
                                    "netty-common-4.1.94.Final.jar \n" +
                                    "netty-handler-4.1.94.Final.jar \n" +
                                    "netty-handler-proxy-4.1.87.Final.jar \n" +
                                    "netty-resolver-4.1.94.Final.jar \n" +
                                    "netty-resolver-dns-4.1.94.Final.jar \n" +
                                    "netty-transport-4.1.94.Final.jar \n" +
                                    "netty-transport-native-unix-common-4.1.87.Final.jar \n" +
                                    "perfmark-api-0.26.0.jar \n" +
                                    "protobuf-java-3.22.3.jar \n" +
                                    "protobuf-java-util-3.22.3.jar \n" +
                                    "proto-google-common-protos-2.17.0.jar \n" +
                                    "slf4j-api-2.0.7.jar \n" +
                                    "vertx-core-4.4.4.jar \n" +
                                    "vertx-grpc-4.4.4.jar \n" +
                                    "2. Put the jar package in the first step in the ${tongweb.home}/lib or ${tongweb.base}/lib directory; \n" +
                                    "3. Start TongWeb, access the console, and enter the connection information in the service registration. Note: You need to keep \"Support centralized management\" in \"Global Configuration\" turned on."});
        }
    }

    @ModelField(unique = true, nameI18n = {"支持产品名", "en:Support Product"}, showToList = true, infoI18n = {"TongWeb 可以支持的产品名称。", "en:Product names that TongWeb can support."})
    public String id;

    @ModelField(nameI18n = {"概要", "en:Summary"}, showToList = true, infoI18n = {"TongWeb 支持该产品的简单介绍。", "en:TongWeb supports a brief summary to the product."})
    public String summary;

    @ModelField(nameI18n = {"使用说明", "en:Usage"}, infoI18n = {"TongWeb 支持该产品的使用说明。", "en:TongWeb supports the instructions for use of this product."})
    public String usage;

    public String downloadName;

    public SupportList() {
    }

    public SupportList(String name, String summary, String usage, String downloadName) {
        this(name, summary, usage);
        this.downloadName = downloadName;
    }

    public SupportList(String id, String summary, String usage) {
        this.id = id;
        this.summary = summary;
        this.usage = usage;
    }

    private SupportList[] supportList() { // NOTE: 放在方法里面，保证国际化可以改变其中的值
        return new SupportList[]{
                new SupportList(snmp, getConsoleContext().getI18N(SupportList.summaryPrefix + snmp), getConsoleContext().getI18N(SupportList.usagePrefix + snmp)),
                new SupportList(prometheus, getConsoleContext().getI18N(SupportList.summaryPrefix + prometheus), getConsoleContext().getI18N(SupportList.usagePrefix + prometheus)),
                new SupportList(pinpoint, getConsoleContext().getI18N(SupportList.summaryPrefix + pinpoint), getConsoleContext().getI18N(SupportList.usagePrefix + pinpoint), "pinpoint"),
                new SupportList(skyWalking, getConsoleContext().getI18N(SupportList.summaryPrefix + skyWalking), getConsoleContext().getI18N(SupportList.usagePrefix + skyWalking), "skywalking"),
                new SupportList(zabbix, getConsoleContext().getI18N(SupportList.summaryPrefix + zabbix), getConsoleContext().getI18N(SupportList.usagePrefix + zabbix), "zabbix"),
                new SupportList(redis, getConsoleContext().getI18N(SupportList.summaryPrefix + redis), getConsoleContext().getI18N(SupportList.usagePrefix + redis)),
                new SupportList(Elasticsearch, getConsoleContext().getI18N(SupportList.summaryPrefix + Elasticsearch), getConsoleContext().getI18N(SupportList.usagePrefix + Elasticsearch)),
                new SupportList(nacos, getConsoleContext().getI18N(SupportList.summaryPrefix + nacos), getConsoleContext().getI18N(SupportList.usagePrefix + nacos)),
                new SupportList(zk, getConsoleContext().getI18N(SupportList.summaryPrefix + zk), getConsoleContext().getI18N(SupportList.usagePrefix + zk)),
                new SupportList(ectd, getConsoleContext().getI18N(SupportList.summaryPrefix + ectd), getConsoleContext().getI18N(SupportList.usagePrefix + ectd)),
                new SupportList(ncs, getConsoleContext().getI18N(SupportList.summaryPrefix + ncs), getConsoleContext().getI18N(SupportList.usagePrefix + ncs)),
        };
    }

    @Override
    public List<Map<String, String>> listInternal(Request request, int start, int size) throws Exception {
        SupportList[] supportLists = supportList();
        Arrays.sort(supportLists, Comparator.comparing(o -> o.id));
        int end = Integer.min(supportLists.length, start + size);
        List<Map<String, String>> results = new ArrayList<>();
        for (int i = start; i < end; i++) {
            results.add(mapper(supportLists[i]));
        }
        return results;
    }

    @Override
    public int getTotalSize(Request request) throws Exception {
        return supportList().length;
    }

    @Override
    public void show(Request request, Response response) throws Exception {
        for (SupportList support : supportList()) {
            if (support.id.equals(request.getId())) {
                response.addDataObject(support);
                break;
            }
        }
    }

    @Override
    public Map<String, List<String[]>> downloadlist0(Request request, Response response) throws Exception {
        List<String[]> files = new ArrayList<>();

        HashMap<String, List<String[]>> result = new HashMap<>();
        String downloadName = getDownloadName(request.getId());
        if (StringUtil.notBlank(downloadName)) {
            File tool = FileUtil.newFile(ConsoleWarHelper.getLibDir(), "tools", downloadName);
            if (tool.isDirectory()) {
                int subPathLen = (tool.getAbsolutePath() + File.separator).length();
                for (File f : Objects.requireNonNull(tool.listFiles())) {
                    files.add(new String[]{f.getAbsolutePath().substring(subPathLen), FileUtil.getFileSize(f)});
                }
                result.put(downloadName, files);
            }
        }
        return result;
    }

    private String getDownloadName(String supportName) {
        for (SupportList support : supportList()) {
            if (support.id.equals(supportName)) {
                if (StringUtil.notBlank(support.downloadName)) {
                    return support.downloadName;
                }
            }
        }
        return null;
    }

    @Override
    public List<File> downloadfile0(Request request, String downloadFileNames) throws Exception {
        if (StringUtil.isBlank(downloadFileNames)) {
            return null;
        }

        List<File> downloadFiles = new ArrayList<>();
        String[] files = downloadFileNames.split(Constants.DATA_SEPARATOR);
        String downloadName = getDownloadName(request.getId());
        if (StringUtil.notBlank(downloadName)) {
            File tool = FileUtil.newFile(ConsoleWarHelper.getLibDir(), "tools", downloadName);
            for (String file : files) {
                downloadFiles.add(FileUtil.newFile(tool, file));
            }
        }

        return downloadFiles;
    }
}
