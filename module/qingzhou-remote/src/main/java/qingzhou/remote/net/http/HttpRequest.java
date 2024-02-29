package qingzhou.remote.net.http;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRequest {

    private String protocol = "HTTP/1.1";
    private String method = "";
    private String path = "";
    private String url = "";
    private String version = "";
    private String contentType = "";
    private String queryString = "";
    private String clientIP;
    private Map<String, String[]> paramMap = new HashMap<>(32);
    private Map<String, String> headerMap = new LinkedHashMap<>(32);
    private byte[] body = new byte[0];

    public HttpRequest() {
        headerMap = new HashMap<>(10);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(String method) {
        this.method = method.toUpperCase();
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return this.version;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public Map<String, String[]> getParamMap() {
        return this.paramMap;
    }

    public void setParamMap(Map<String, String[]> paramMap) {
        this.paramMap = paramMap;
    }

    public Map<String, String> getHeaderMap() {
        return this.headerMap;
    }

    public void setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getParam(String name) {
        if (!this.paramMap.containsKey(name)) {
            return null;
        }
        return this.paramMap.get(name).length == 0 ? "" : this.paramMap.get(name)[0];
    }

    public String getParam(String name, String defaultValue) {
        if (!this.paramMap.containsKey(name) || this.paramMap.get(name).length == 0) {
            return defaultValue;
        }
        return this.paramMap.get(name)[0];
    }

    public String[] getParams(String name) {
        if (!this.paramMap.containsKey(name)) {
            return null;
        }
        return this.paramMap.get(name);
    }

    public String[] getParams(String name, String[] defaultValues) {
        if (!this.paramMap.containsKey(name) || this.paramMap.get(name).length == 0) {
            return defaultValues;
        }
        return this.paramMap.get(name);
    }
}
