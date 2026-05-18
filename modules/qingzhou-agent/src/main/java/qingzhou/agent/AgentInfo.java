package qingzhou.agent;

import java.util.Dictionary;
import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.AiTool;

@Component(property = AiTool.TOOL_DESCRIPTION + "=获取本地代理 Agent 程序的配置信息。" +
        "返回结果包含：本地监听的端口、集中管理平台的URL、向平台注册应用的心跳周期（秒）。",
        configurationPid = "qingzhou-agent", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AgentInfo implements AiTool {
    @Reference
    private ConfigurationAdmin configAdmin;

    private String serverPort;
    private String url;
    private String interval;

    @Activate
    public void start(Map<String, String> config) throws Exception {
        url = config.get("url");
        interval = config.get("interval");

        Dictionary<String, Object> httpServerConfig = configAdmin.getConfiguration("qingzhou-http-server", null).getProperties();
        serverPort = String.valueOf(httpServerConfig.get("port"));
    }

    @Override
    public Object invoke(Map<String, Object> toolArgs) {
        return String.format("本地监听的端口: %s\n " +
                        "集中管理平台的URL: %s\n " +
                        "向平台注册应用的心跳周期（秒）: %s",
                serverPort, url, interval);
    }
}
