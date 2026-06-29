package qingzhou.registry.web;

import java.util.*;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.AiTool;
import qingzhou.ai.SystemAiTool;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=/instance",
        AiTool.TOOL_DESCRIPTION + "=该接口返回轻舟平台上注册的所有轻舟实例的列表信息，每个实例包含实例ID和所在服务器的IP地址等信息。"})
public class Instance implements HttpHandler, SystemAiTool {
    @Reference
    private Registry registry;

    @Reference
    private Json json;

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
                result.add(new HashMap<String, String>() {{
                    put(WebUtil.INSTANCE_ID, info.getId());
                    put("host", info.getHost());
                    put("port", String.valueOf(info.getPort()));
                    put("version", info.getVersion());
                }});
            }

            return result;
        }
    };

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        // 是否已缓存
        if (WebUtil.cached(httpRequest, httpResponse, registry)) return;

        // 执行
        WebUtil.sendResult(function, httpRequest, httpResponse, registry, json);
    }

    @Override
    public String invoke(Map<String, Object> toolArgs) throws Exception {
        return json.toJson(function.apply(null));
    }
}