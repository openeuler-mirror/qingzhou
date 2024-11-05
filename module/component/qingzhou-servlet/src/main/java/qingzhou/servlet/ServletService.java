package qingzhou.servlet;

import qingzhou.engine.ServiceInfo;

public interface ServletService extends ServiceInfo {
    @Override
    default String getDescription() {
        return "Provide practical tools related to Servlet.";
    }

    ServletContainer createServletContainer();
}
