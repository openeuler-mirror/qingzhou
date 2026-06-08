package qingzhou.http.client.impl;

import java.util.Map;

import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Request;

class RequestImpl implements Request {
    String url;
    HttpMethod method;
    Map<String, String> headers;
    Map<String, String> params;
    byte[] body;
    Map<String, String> files;

    public RequestImpl(String url) {
        this.url = url;
    }

    @Override
    public Request method(HttpMethod method) {
        this.method = method;
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
    public Request files(Map<String, String> files) {
        this.files = files;
        return this;
    }
}
