package qingzhou.console.controller.rest;

import qingzhou.deployer.RequestImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RestContext {
    public final HttpServletRequest req;
    public final HttpServletResponse resp;
    public final RequestImpl request;
    public final List<String> batchIds;

    public RestContext(HttpServletRequest req, HttpServletResponse resp, RequestImpl request) {
        this.req = req;
        this.resp = resp;
        this.request = request;
        this.batchIds = request.getParameter("id") != null ? Arrays.asList(request.getParameter("id").split(",")) : new ArrayList<>();
    }
}
