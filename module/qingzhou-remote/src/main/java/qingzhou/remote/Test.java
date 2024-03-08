package qingzhou.remote;

import qingzhou.framework.util.FileUtil;
import qingzhou.remote.http.HttpContext;
import qingzhou.remote.http.HttpExchange;
import qingzhou.remote.http.HttpHandler;
import qingzhou.remote.http.HttpServer;
import qingzhou.remote.http.sun.HttpServerImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;

public class Test {
    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServerImpl();
        server.start("0.0.0.0", 7000, 0);
        HttpContext context = server.createContext("/");
        context.setHandler(new TestHandler());
    }

    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("Request URI: " + exchange.getRequestURI());
            System.out.println("Request URI decoded: " + URLDecoder.decode(exchange.getRequestURI(), "UTF-8"));

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileUtil.copyStream(exchange.getRequestBody(), bos);
            System.out.println("Request Body: " + bos);

            exchange.addResponseHeader("Content-Type", "application/json;charset=UTF-8");
            exchange.setStatus(200);
            String content = "{" +
                    "   \"sites\": [" +
                    "        {\"name\":\"google\" ,\"url\":\"www.google.com\" }, " +
                    "        {\"name\":\"微博\" ,\"url\":\"www.weibo.com\" }" +
                    "    ]" +
                    "}";
            exchange.getResponseBody().write(content.getBytes());
            exchange.close();
        }
    }
}
