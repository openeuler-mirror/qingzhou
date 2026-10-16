package qingzhou.registry.web;

import java.util.function.Function;

import org.osgi.service.component.annotations.Reference;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.Registry;

/**
 * 元数据查询 handler 公共模板：缓存命中短路 + 统一响应格式
 */
abstract class RegistryMetaHandler implements HttpHandler {
    @Reference
    protected Registry registry;
    @Reference
    protected Json json;

    abstract Function<HandlingContext, Object> getFunction();

    @Override
    public final void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        if (WebUtil.isCacheHit(httpRequest, httpResponse, registry)) return; // 是否已缓存
        WebUtil.sendResult(getFunction(), httpRequest, httpResponse, registry, json); // 执行
    }
}
