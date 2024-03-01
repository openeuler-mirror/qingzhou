package qingzhou.console;

import qingzhou.framework.app.RequestImpl;
import qingzhou.framework.app.ResponseImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestContext {
    public final HttpServletRequest servletRequest;
    public final HttpServletResponse servletResponse;
    public final RequestImpl request;
    public ResponseImpl response;

    public RestContext(HttpServletRequest servletRequest, HttpServletResponse servletResponse, RequestImpl request, ResponseImpl response) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        this.request = request;
        this.response = response;
    }
}
