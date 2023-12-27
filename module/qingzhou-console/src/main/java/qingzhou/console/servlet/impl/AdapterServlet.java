package qingzhou.console.servlet.impl;

import qingzhou.console.servlet.ServletProcessor;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AdapterServlet extends HttpServlet {
    private final ServletProcessor processor;

    public AdapterServlet(ServletProcessor processor) {
        this.processor = processor;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        processor.process(new ServletRequestContext(req, resp));
    }
}
