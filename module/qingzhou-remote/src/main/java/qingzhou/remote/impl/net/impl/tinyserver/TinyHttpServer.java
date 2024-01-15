package qingzhou.remote.impl.net.impl.tinyserver;

import qingzhou.remote.impl.net.HttpHandler;
import qingzhou.remote.impl.net.HttpRequest;
import qingzhou.remote.impl.net.HttpResponse;
import qingzhou.remote.impl.net.HttpRoute;
import qingzhou.remote.impl.net.HttpServer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TinyHttpServer implements HttpServer {

    private static final int DEFAULT_PORT = 7000;
    private static final int DEFAULT_BACKLOG = 1024;

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private int port = DEFAULT_PORT;
    private int backlog = DEFAULT_BACKLOG;
    private final int DEFAULT_BUFFER_SIZE = 4096;
    private final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private final String PROTOCOL_SUPPORTED = "HTTP/1.1";
    private final List<HttpRoute> ROUTES = new ArrayList<>();
    private final Map<String, HttpHandler> ROUTE_CONFIG = new HashMap<>(16);
    private final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final ExecutorService THREADPOOL = Executors.newFixedThreadPool(5);
    private final Set<String> METHODS = new HashSet<>(Arrays.asList("GET", "HEAD", "POST", "DELETE", "PUT"));

    public TinyHttpServer() {
        this(DEFAULT_PORT, DEFAULT_BACKLOG);
    }

    public TinyHttpServer(int port) {
        this(port, DEFAULT_BACKLOG);
    }

    public TinyHttpServer(int port, int backlog) {
        this.port = port;
        this.backlog = backlog;
    }

    @Override
    public void addContext(HttpRoute httpRoute, HttpHandler httpHandler) {
        for (HttpRoute item : ROUTES) {
            if (item.getUrl().equals(httpRoute.getUrl())) {
                throw new IllegalArgumentException("The route path already exist!");
            }
        }
        ROUTE_CONFIG.put(httpRoute.getUrl(), httpHandler);
        ROUTES.add(httpRoute);
    }

    @Override
    public void removeContext(String path) {
        ROUTE_CONFIG.remove(path);
    }

    @Override
    public void start() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port), backlog);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            EXECUTOR.submit(new Thread(() -> {
                try {
                    poll(selector);
                } catch (IOException e) {
                    System.out.println("TinyHttpServer shutdown terminated！");
                    printStackTrace(e);
                }
            }, "TinyHttpServer-" + this.port));
            System.out.println("TinyHttpServer started with port:" + this.port);
        } catch (IOException e) {
            System.out.println("TinyHttpServer start failed！");
            printStackTrace(e);
        }
    }

    @Override
    public void stop(int delaySeconds) {
        if (!EXECUTOR.isShutdown()) {
            try {
                selector.close();
                serverSocketChannel.close();
                System.out.println("TinyHttpServer shutdown success.");
            } catch (IOException e) {
                System.out.println("TinyHttpServer shutdown exception.");
                printStackTrace(e);
            } finally {
                EXECUTOR.shutdown();
            }
        }
    }

    /**
     * 轮询键集
     *
     * @param selector
     * @throws IOException
     */
    private void poll(Selector selector) throws IOException {
        while (true) {
            int readyNum = selector.select();
            if (readyNum == 0) {
                continue;
            }
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                if (selectionKey.isAcceptable()) {
                    handleAccept(selectionKey);
                } else if (selectionKey.isReadable()) {
                    handleRead(selectionKey);
                } else if (selectionKey.isWritable()) {
                    handleWrite(selectionKey);
                }
                iterator.remove();
            }
        }
    }

    private void handleAccept(SelectionKey key) {
        try {
            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = channel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(key.selector(), SelectionKey.OP_READ);
        } catch (IOException e) {
            printStackTrace(e);
        }
    }

    private void handleRead(SelectionKey key) {
        try {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            // 1. 读取数据
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            read(socketChannel, out);
            if (out.size() == 0) {
                socketChannel.close();
                return;
            }
            // 2. 解码
            final HttpRequest request = decode(out.toByteArray());
            String ip = socketChannel.getRemoteAddress().toString().replace("/", "");
            if (ip != null) {
                request.setClientIP(ip.contains(":") ? ip.substring(0, ip.indexOf(":")) : ip);
            }
            // 3. 业务处理
            THREADPOOL.submit(() -> {
                HttpResponse response = new HttpResponse(new ByteArrayOutputStream());
                HttpRoute matchRoute = new HttpRoute(request.getPath(), request.getMethod()).match(ROUTES);
                if (matchRoute == null) {
                    response.setStatusCode(StatusCode.NOT_FOUND.getCode());
                    response.setStatusMessage(StatusCode.NOT_FOUND.getDetail());
                } else {
                    try {
                        ROUTE_CONFIG.get(matchRoute.getUrl()).handle(request, response);
                        response.setStatusCode(StatusCode.OK.getCode());
                        response.setStatusMessage(StatusCode.OK.getDetail());
                    } catch (Exception e) {
                        response.setStatusCode(StatusCode.INTERNAL_ERR.getCode());
                        response.setStatusMessage(StatusCode.INTERNAL_ERR.getDetail());
                        printStackTrace(e);
                    }
                }
                key.attach(response);
                // 获得响应
                key.interestOps(SelectionKey.OP_WRITE);
                // 坑：异步唤醒
                key.selector().wakeup();
                //socketChannel.register(key.selector(), SelectionKey.OP_WRITE, response);
            });
        } catch (IOException e) {
            printStackTrace(e);
        }
    }

    /**
     * 从缓冲区读取数据并写入 {@link ByteArrayOutputStream}
     *
     * @param socketChannel
     * @param out
     * @throws IOException
     */
    private void read(SocketChannel socketChannel, ByteArrayOutputStream out) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        while (socketChannel.read(buffer) > 0) {
            buffer.flip(); // 切换到读模式
            out.write(buffer.array());
            buffer.clear(); // 清理缓冲区
        }
    }

    /**
     * 解码 Http 请求报文
     *
     * @param array
     * @return
     */
    private HttpRequest decode(byte[] array) {
        try {
            HttpRequest request = new HttpRequest();
            ByteArrayInputStream inStream = new ByteArrayInputStream(array);
            InputStreamReader reader = new InputStreamReader(inStream, DEFAULT_CHARSET);
            BufferedReader in = new BufferedReader(reader);

            // 解析起始行 如：GET /index HTTP/1.1
            String firstLine = in.readLine();
            String[] split = firstLine.split(" ");
            request.setMethod(split[0].toUpperCase());
            request.setUrl(URLDecoder.decode(split[1], DEFAULT_CHARSET.name()));
            request.setProtocol(split[2]);
            if (split[1].contains("?")) {
                String url = URLDecoder.decode(split[1], DEFAULT_CHARSET.name());
                request.setPath(url.substring(0, url.indexOf("?")));
                request.setQueryString(url.substring(url.indexOf("?")).replaceFirst("\\?", ""));
            } else {
                request.setPath(split[1]);
                request.setQueryString("");
            }

            if (!PROTOCOL_SUPPORTED.equals(request.getProtocol())) {
                printStackTrace(new IllegalStateException("Http protocol [" + request.getProtocol() + "] not supported."));
            }
            if (!METHODS.contains(request.getMethod())) {
                printStackTrace(new IllegalStateException("Http method [" + request.getMethod() + "] not supported."));
            }

            // 解析请求头
            while (true) {
                String line = in.readLine();
                // 请求头以一个空行结束
                if ("".equals(line.trim())) {
                    break;
                }
                String[] keyValue = line.split(":");
                request.getHeaderMap().put(keyValue[0], keyValue[1]);
                if ("Content-Type".equalsIgnoreCase(keyValue[0])) {
                    request.setContentType(keyValue[1]);
                }
            }

            // 解析参数
            if (request.getQueryString().length() > 0) {
                Map<String, String[]> paramMap = new HashMap<>();
                String[] items = request.getQueryString().split("&");
                for (String item : items) {
                    String key = item.contains("=") ? item.substring(0, item.indexOf("=")) : item;
                    String[] values = paramMap.getOrDefault(key, new String[0]);
                    String[] newValues = Arrays.copyOf(values, values.length + 1);
                    newValues[values.length] = item.contains("=") ? item.substring(item.indexOf("=") + 1) : "";
                    paramMap.put(key, newValues);
                }
                request.setParamMap(paramMap);
            }

            // 解析请求主体
            CharArrayWriter writer = new CharArrayWriter();
            CharBuffer charBuffer = CharBuffer.allocate(DEFAULT_BUFFER_SIZE);
            while (in.read(charBuffer) > 0) {
                charBuffer.flip();
                char[] chars = charBuffer.array();
                writer.write(chars);
                charBuffer.clear();
            }
            //request.setBody(byteBuffer.array());
            request.setBody(new String(writer.toCharArray()).getBytes(DEFAULT_CHARSET));
            return request;
        } catch (IOException e) {
            printStackTrace(e);
        }
        return null;
    }

    private void handleWrite(SelectionKey key) throws IOException {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            HttpResponse response = (HttpResponse) key.attachment();
            // 编码
            byte[] bytes = encode(response);
            channel.write(ByteBuffer.wrap(bytes));
            // 取消对事件感兴趣 (key.cancel(); 或 key.interestOps(SelectionKey.OP_READ);)
            key.interestOps(SelectionKey.OP_READ);
            key.attach(null);
        } catch (IOException e) {
            printStackTrace(e);
        }
    }

    /**
     * http 响应报文编码
     *
     * @param response
     * @return
     */
    private byte[] encode(HttpResponse response) {
        StringBuilder builder = new StringBuilder();
        try {
            response.setContentLength(response.getContent().getBytes(Charset.forName(response.getCharacterEncoding())).length);
            builder.append(response.getProtocol()).append(" ").append(response.getStatusCode()).append(" ").append(response.getStatusMessage()).append((char) 0x0d).append((char) 0x0a);
            for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
                String entryValue = entry.getValue();
                if ("Content-Type".equalsIgnoreCase(entry.getKey()) && !entryValue.toLowerCase().contains("charset")) {
                    entryValue = entryValue + ";charset=" + response.getCharacterEncoding();
                }
                builder.append(entry.getKey()).append(": ").append(entryValue).append((char) 0x0d).append((char) 0x0a);
            }
            builder.append((char) 0x0d).append((char) 0x0a);
            builder.append(response.getContent());
            builder.append((char) 0x0d).append((char) 0x0a);
        } catch (Exception e) {
            response.setStatusCode(StatusCode.INTERNAL_ERR.getCode());
            response.setStatusMessage(StatusCode.INTERNAL_ERR.getDetail());
            builder.setLength(0);
            builder.append("<h2>500 Interval error.</h2>");
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer, true));
            response.setStatusCode(500);
            builder.append(writer.toString());
            response.setContent(builder.toString());
            response.setContentLength(response.getContent().getBytes(Charset.forName(response.getCharacterEncoding())).length);
            builder.append(response.getProtocol()).append(" ").append(response.getStatusCode()).append(" ").append(response.getStatusMessage()).append((char) 0x0d).append((char) 0x0a);
            for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
                String entryValue = entry.getValue();
                if ("Content-Type".equalsIgnoreCase(entry.getKey()) && !entryValue.toLowerCase().contains("charset")) {
                    entryValue = entryValue + ";charset=" + response.getCharacterEncoding();
                }
                builder.append(entry.getKey()).append(": ").append(entryValue).append((char) 0x0d).append((char) 0x0a);
            }
            builder.append((char) 0x0d).append((char) 0x0a);
            builder.append(response.getContent());
            builder.append((char) 0x0d).append((char) 0x0a);

            printStackTrace(e);
        }

        //return DEFAULT_CHARSET.encode(CharBuffer.wrap(builder.toString())).array();
        return builder.toString().getBytes(DEFAULT_CHARSET);
    }

    private void printStackTrace(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer, true));
        System.out.println(writer.getBuffer().toString());
    }

    private enum StatusCode {
        OK(200, "OK", ""),
        BAD_REQ(400, "Bad Request", "Bad Request"),
        FORBIDDEN(403, "Access Forbidden", "Access Forbidden"),
        NOT_FOUND(404, "Not Found", "Not Found"),
        INTERNAL_ERR(500, "Internal Error", "Internal Error");

        private final int code;
        private final String name;
        private final String detail;

        private StatusCode(int code, String name, String detail) {
            this.code = code;
            this.name = name;
            this.detail = detail;
        }

        public int getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDetail() {
            return detail;
        }

        public StatusCode valueOf(int code) {
            for (StatusCode item : StatusCode.values()) {
                if (item.getCode() == code) {
                    return item;
                }
            }
            return StatusCode.OK;
        }
    }
}
