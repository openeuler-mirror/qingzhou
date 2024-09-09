package qingzhou.app.system.service;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Listable;
import qingzhou.app.system.Main;
import qingzhou.config.Agent;
import qingzhou.config.Config;
import qingzhou.deployer.DeployerConstants;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(code = "instance", icon = "stack",
        menu = Main.SERVICE_MENU, order = 2,
        name = {"实例", "en:Instance"},
        info = {"实例是应用部署的载体，为应用提供运行时环境。预置的 " + DeployerConstants.INSTANCE_LOCAL + " 实例表示当前正在访问的服务所在的实例，如集中管理端就运行在此实例上。",
                "en:An instance is the carrier of application deployment and provides a runtime environment for the application. The provisioned " + DeployerConstants.INSTANCE_LOCAL + " instance indicates the instance where the service is currently accessed, such as the centralized management side running on this instance."})
public class Instance extends ModelBase implements Listable {
    @ModelField(
            required = true,
            list = true,
            name = {"实例名称", "en:Name"},
            info = {"表示该实例的名称，用于识别和管理该实例。",
                    "en:Indicates the name of the instance, which is used to identify and manage the instance."})
    public String name;

    @ModelField(
            required = true,
            host = true,
            list = true,
            name = {"主机IP", "en:Host IP"},
            info = {"该实例所在服务器的域名或 IP 地址。",
                    "en:The domain name or IP address of the server where the instance resides."})
    public String host;

    @ModelField(
            required = true,
            port = true,
            list = true,
            name = {"管理端口", "en:Management Port"},
            info = {"该实例所开放的管理端口，用以受理轻舟集中管理端发来的业务请求。",
                    "en:The management port opened by the instance is used to accept business requests from the centralized management end of Qingzhou."})
    public Integer port;

    @ModelField(
            monitor = true,
            list = true,
            name = {"运行中", "en:Running"},
            info = {"用以表示该实例是否正在运行。",
                    "en:This indicates whether the instance is running."})
    public Boolean running;

    @Override
    public String idFieldName() {
        return "name";
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) {
        List<Map<String, String>> result = new ArrayList<>();

        Registry registry = Main.getService(Registry.class);

        result.add(localInstance());

        registry.getAllInstanceNames().forEach(s -> {
            InstanceInfo instanceInfo = registry.getInstanceInfo(s);
            result.add(new HashMap<String, String>() {{
                put(idFieldName(), instanceInfo.getName());
                put("host", instanceInfo.getHost());
                put("port", String.valueOf(instanceInfo.getPort()));
            }});
        });
        return result;
    }

    @Override
    public int totalSize() {
        return Main.getService(Registry.class).getAllAppNames().size()
                + 1;// +1 local instance
    }

    @Override
    public Map<String, String> showData(String id) {
        if (id.equals(DeployerConstants.INSTANCE_LOCAL)) return localInstance();

        Registry registry = Main.getService(Registry.class);
        InstanceInfo instanceInfo = registry.getInstanceInfo(id);
        if (instanceInfo == null) return null;
        return new HashMap<String, String>() {{
            put(idFieldName(), instanceInfo.getName());
            put("host", instanceInfo.getHost());
            put("port", String.valueOf(instanceInfo.getPort()));
        }};
    }

    private Map<String, String> localInstance() {
        return new HashMap<String, String>() {{
            put(idFieldName(), DeployerConstants.INSTANCE_LOCAL);
            put("host", "localhost");

            Config config = Main.getService(Config.class);
            Agent agent = config.getAgent();
            put("port", agent.isEnabled() ? String.valueOf(agent.getAgentPort()) : "--");
        }};
    }
}
