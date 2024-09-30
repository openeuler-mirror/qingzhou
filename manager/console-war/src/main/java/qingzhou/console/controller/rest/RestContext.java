package qingzhou.console.controller.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.deployer.RequestImpl;

public class RestContext {
    public final HttpServletRequest req;
    public final HttpServletResponse resp;
    public final RequestImpl request;
    public String[] batchIds;

    public RestContext(HttpServletRequest req, HttpServletResponse resp, RequestImpl request) {
        this.req = req;
        this.resp = resp;
        this.request = request;
    }
}
