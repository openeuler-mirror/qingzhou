package qingzhou.console.controller.rest;

import qingzhou.deployer.RequestImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestContext {
    public final HttpServletRequest servletRequest;
    public final HttpServletResponse servletResponse;
    public final RequestImpl request;

    public RestContext(HttpServletRequest servletRequest, HttpServletResponse servletResponse, RequestImpl request) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        this.request = request;
    }
}
