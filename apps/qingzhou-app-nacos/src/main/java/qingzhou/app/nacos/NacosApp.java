package qingzhou.app.nacos;


import java.util.*;
import qingzhou.api.*;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;

@App(icon = "/icons/logo.png",
        name = {"Nacos应用", "en:Nacos Application"},
        info = {"Nacos配置中心和服务注册发现管理应用", "en:Nacos configuration center and service discovery management application."})
public class NacosApp implements QingzhouApp {

    private HttpClient httpClient;
    private Json json;
    private NacosApi nacosApi;
    private String nacosServerAddr;
    private String nacosUsername;
    private String nacosPassword;

    @Override
    public void start(AppContext appContext) {
        Properties props = appContext.getProperties();
        this.nacosServerAddr = props.getProperty("nacos.serverAddr", "http://localhost:8848");
        this.nacosUsername = props.getProperty("nacos.username", "nacos");
        this.nacosPassword = props.getProperty("nacos.password", "nacos");
        this.httpClient = appContext.getService(HttpClient.class);
        this.json = appContext.getService(Json.class);
        this.nacosApi = new NacosApi(httpClient, json, nacosServerAddr, nacosUsername, nacosPassword);
    }
    public Json getJson() {
        return json;
    }

    public NacosApi getNacosApi() {
        return nacosApi;
    }
}