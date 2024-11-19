package qingzhou.servlet.impl;

import qingzhou.servlet.ServletContainer;
import qingzhou.servlet.ServletService;

public class ServletServiceImpl implements ServletService {
    @Override
    public ServletContainer createServletContainer() {
        return new ServletContainerImpl();
    }
}
