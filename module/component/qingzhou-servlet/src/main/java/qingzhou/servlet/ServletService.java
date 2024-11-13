package qingzhou.servlet;

import qingzhou.engine.Service;

@Service(name = "Servlet Container", description = "A servlet container that deploys applications developed based on the servlet specification.")
public interface ServletService {
    ServletContainer createServletContainer();
}
