package qingzhou.http.client.impl;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Request;

class RequestImpl implements Request {
    private final boolean allowSslTrustAll;

    String url;
    HttpMethod method;
    Map<String, String> headers;
    Map<String, String> params;
    byte[] body;
    Map<String, List<String>> files;
    X509Certificate[] trustedCertificates;
    boolean trustAll;
    int connectTimeout = 60 * 1000;
    int readTimeout = 10 * 60 * 1000;
    int maxBodySize = 64 * 1024 * 1024;

    RequestImpl(String url, boolean allowSslTrustAll) {
        this.url = url;
        this.allowSslTrustAll = allowSslTrustAll;
    }

    @Override
    public Request method(HttpMethod method) {
        this.method = method;
        return this;
    }

    @Override
    public Request header(String key, String val) {
        if (headers == null) headers = new HashMap<>();
        headers.put(key, val);
        return this;
    }

    @Override
    public Request headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public Request params(Map<String, String> params) {
        this.params = params;
        return this;
    }

    @Override
    public Request body(byte[] body) {
        this.body = body;
        return this;
    }

    @Override
    public Request files(Map<String, List<String>> files) {
        this.files = files;
        return this;
    }

    @Override
    public Request connectTimeout(int connectTimeout) {
        if (connectTimeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }

        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public Request readTimeout(int readTimeout) {
        if (readTimeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }

        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public Request trustedCertificates(X509Certificate... certificates) {
        this.trustedCertificates = certificates;
        return this;
    }

    @Override
    public Request trustAllCertificates() {
        // 跳过证书与主机名校验属高危能力：须由运维显式开启系统属性，避免代码侧误用造成静默 MITM
        if (!allowSslTrustAll) {
            throw new IllegalStateException("trustAllCertificates is disabled, enable it with qingzhou-http-client.allow_ssl_trust_all=true");
        }
        this.trustAll = true;
        return this;
    }

    @Override
    public Request maxBodySize(int maxBodySize) {
        if (maxBodySize < 0) {
            throw new IllegalArgumentException("maxBodySize can not be negative");
        }

        this.maxBodySize = maxBodySize;
        return this;
    }
}
