package qingzhou.console.controller.rest;

import qingzhou.deployer.RequestImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestContext {
    public final HttpServletRequest req;
    public final HttpServletResponse resp;
    public RequestImpl request;

    public RestContext(HttpServletRequest req, HttpServletResponse resp, RequestImpl request) {
        this.req = req;
        this.resp = resp;
        this.request = request;
    }
}
