package qingzhou.http;

import qingzhou.engine.Service;

@Service(name = "HTTP Implementation", description = "Provides easy implementation of the HTTP protocol on both the client and server side.")
public interface Http {
    HttpServer buildHttpServer();

    HttpClient buildHttpClient();
}
