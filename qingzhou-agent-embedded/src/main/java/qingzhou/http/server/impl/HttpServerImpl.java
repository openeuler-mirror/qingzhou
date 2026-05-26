package qingzhou.http.server.impl;

import com.sun.net.httpserver.HttpExchange;
import qingzhou.http.server.HttpHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServerImpl implements qingzhou.http.server.HttpServer {
    private com.sun.net.httpserver.HttpServer server;
    private final int port;
    private final Map<String, HttpHandler> handlers = new ConcurrentHashMap<>();
    private final Map<HttpHandler, String> handlerPaths = new ConcurrentHashMap<>();

    public HttpServerImpl(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::dispatch);
        int threads = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(threads, 4));
        server.setExecutor(executor);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Override
    public void registerHttpHandler(HttpHandler httpHandler, String handlePath) {
        handlers.put(handlePath, httpHandler);
        handlerPaths.put(httpHandler, handlePath);
    }

    @Override
    public void unregisterHttpHandler(HttpHandler httpHandler) {
        String path = handlerPaths.remove(httpHandler);
        if (path != null) {
            handlers.remove(path);
        }
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        HttpHandler handler = findHandler(path);
        if (handler != null) {
            try {
                HttpRequestImpl request = new HttpRequestImpl(exchange);
                HttpResponseImpl response = new HttpResponseImpl(exchange);
                handler.handle(request, response);
                if (!response.isFinished()) {
                    response.finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private HttpHandler findHandler(String path) {
        // exact match
        HttpHandler handler = handlers.get(path);
        if (handler != null) return handler;
        // prefix match (longest prefix first)
        String bestMatch = null;
        for (String prefix : handlers.keySet()) {
            if (path.startsWith(prefix)) {
                if (bestMatch == null || prefix.length() > bestMatch.length()) {
                    bestMatch = prefix;
                }
            }
        }
        return bestMatch != null ? handlers.get(bestMatch) : null;
    }
}