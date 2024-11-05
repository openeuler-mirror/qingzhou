package qingzhou.http;

import qingzhou.engine.ServiceInfo;

public interface Http extends ServiceInfo {
    @Override
    default String getDescription() {
        return "Provide practical tools related to Http.";
    }

    HttpServer buildHttpServer();

    HttpClient buildHttpClient();
}
