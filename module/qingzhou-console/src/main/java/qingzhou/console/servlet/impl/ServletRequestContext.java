package qingzhou.console.servlet.impl;

import qingzhou.console.servlet.RequestContext;
import qingzhou.console.servlet.UploadFileContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ServletRequestContext implements RequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;


    public ServletRequestContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public String getRequestURI() {
        return request.getRequestURI();
    }

    @Override
    public String[] getParameterValues(String name) {
        return request.getParameterValues(name);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public UploadFileContext getUploadContext() throws Exception {
        return new UploadFileContextImpl(request);
    }
}