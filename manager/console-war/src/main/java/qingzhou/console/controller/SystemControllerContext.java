package qingzhou.console.controller;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SystemControllerContext {
    public final HttpServletRequest req;
    public final HttpServletResponse resp;
    public final FilterChain chain;

    public SystemControllerContext(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) {
        this.req = req;
        this.resp = resp;
        this.chain = chain;
    }
}
