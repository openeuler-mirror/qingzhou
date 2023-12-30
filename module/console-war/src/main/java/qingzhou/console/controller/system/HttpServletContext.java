package qingzhou.console.controller.system;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpServletContext {
    public final HttpServletRequest req;
    public final HttpServletResponse resp;
    public final FilterChain chain;

    public HttpServletContext(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) {
        this.req = req;
        this.resp = resp;
        this.chain = chain;
    }
}
