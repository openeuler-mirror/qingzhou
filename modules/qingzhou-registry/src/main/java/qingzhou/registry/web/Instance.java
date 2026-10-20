package qingzhou.registry.web;

import java.util.*;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.json.Json;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/instance",
        service = {Instance.class, HttpHandler.class})
public class Instance extends AbstractMetaHandler {

    @Reference
    protected void setRegistry(Registry registry) {
        this.registry = registry;
    }

    @Reference
    protected void setJson(Json json) {
        this.json = json;
    }

    private final Function<RequestParameters, Object> function = context -> {
        List<InstanceInfo> remoteInstanceList = new ArrayList<>();
        for (String id : registry.getAllRemoteInstances()) {
            InstanceInfo remote = registry.getRemoteInstance(id);
            if (remote == null) continue; // 实例可能被健康检查并发移除
            remoteInstanceList.add(remote);
        }
        remoteInstanceList.sort(Comparator.comparing(InstanceInfo::getHost));
        remoteInstanceList.add(0, registry.getLocalInstance());

        List<Map<String, String>> result = new ArrayList<>();
        for (InstanceInfo info : remoteInstanceList) {
            Map<String, String> item = new HashMap<>();
            item.put(RequestParameters.INSTANCE_ID, info.getId());
            item.put("host", info.getHost());
            item.put("port", String.valueOf(info.getPort()));
            item.put("version", info.getPlatformVersion());
            result.add(item);
        }

        return result;
    };

    public Function<RequestParameters, Object> getFunction() {
        return function;
    }
}