package qingzhou.app.nacos;

import java.util.Map;

import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.api.annotations.App;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;

@App(icon = "/icons/logo.png", roles = "admin",
        name = {"Nacos应用", "en:Nacos Application"},
        info = {"Nacos配置中心和服务注册发现管理应用", "en:Nacos configuration center and service discovery management application."})
public class NacosApp implements QingzhouApp {
    static NacosApi nacosApi;

    @Override
    public void start(AppContext appContext) {
        Map<String, String> props = appContext.getAppConfig();
        String serverAddr = props.getOrDefault("nacos.serverAddr", "http://localhost:8848");
        String username = props.getOrDefault("nacos.username", "nacos");
        String password = props.getOrDefault("nacos.password", "nacos");
        HttpClient httpClient = appContext.getService(HttpClient.class);
        Json json = appContext.getService(Json.class);
        nacosApi = new NacosApi(httpClient, json, serverAddr, username, password);
    }
}
