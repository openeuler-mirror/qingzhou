package qingzhou.registry.web;

import java.util.*;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;

@Component(property = HttpHandler.HANDLE_PATH + "=/instance",
        service = {Instance.class, HttpHandler.class})
public class Instance extends RegistryMetaHandler {


    private final Function<HandlingContext, Object> function = new Function<HandlingContext, Object>() {
        @Override
        public Object apply(HandlingContext context) {
            List<InstanceInfo> remoteInstanceList = new ArrayList<>();
            for (String id : registry.getAllRemoteInstances()) {
                remoteInstanceList.add(registry.getRemoteInstance(id));
            }
            remoteInstanceList.sort(Comparator.comparing(InstanceInfo::getHost));
            remoteInstanceList.add(0, registry.getLocalInstance());

            List<Map<String, String>> result = new ArrayList<>();
            for (InstanceInfo info : remoteInstanceList) {
                Map<String, String> item = new HashMap<>();
                item.put(WebUtil.INSTANCE_ID, info.getId());
                item.put("host", info.getHost());
                item.put("port", String.valueOf(info.getPort()));
                item.put("version", info.getVersion());
                result.add(item);
            }

            return result;
        }
    };

    public Function<HandlingContext, Object> getFunction() {
        return function;
    }
}