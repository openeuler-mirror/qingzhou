package qingzhou.console.controller.rest;

import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestContext {
    public final HttpServletRequest servletRequest;
    public final HttpServletResponse servletResponse;
    public final Request request;
    public Response response;

    public RestContext(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Request request, Response response) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        this.request = request;
        this.response = response;
    }
}
