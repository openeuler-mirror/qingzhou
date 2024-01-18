package qingzhou.remote.impl.net;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpResponse {

    private static final String SERVER_NAME = "TinyHttpServer/0.9.0";
    private String protocol = "HTTP/1.1";
    private int statusCode = 0;
    private String statusMessage;
    private String characterEncoding = "UTF-8";
    private String contentType = "text/plain;charset=" + characterEncoding;
    private long contentLength = 0L;
    private final Map<String, String> HEADERS = new ConcurrentHashMap<>(16);
    private String content = "";
    
    public HttpResponse(ByteArrayOutputStream outputStream) {
        HEADERS.put("Connection", "Keep-Alive");
        HEADERS.put("Content-Type", contentType);
        HEADERS.put("Content-Length", String.valueOf(contentLength));
        HEADERS.put("Server", SERVER_NAME);
        HEADERS.put("Date", new Date().toString());
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
        HEADERS.put("Content-Type", contentType);
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
        HEADERS.put("Content-Length", String.valueOf(contentLength));
    }

    public String[] getHeaderNames() {
        return HEADERS.keySet().stream().toArray(String[]::new);
    }
    
    public Map<String, String> getHeaders() {
        return HEADERS;
    }

    public String getHeader(String name) {
        return HEADERS.get(name);
    }

    public void setHeader(String name, String value) {
        HEADERS.put(name, value);
    }
    
    public void removeHeader(String name) {
        HEADERS.remove(name);
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }
    
    public void dispatch(String path) {
        // TODO
    }
    
    public void redirect(String path) {
        // TODO
    }
}
