package qingzhou.console.impl;

import qingzhou.framework.pattern.Process;
import qingzhou.framework.util.FileUtil;

import java.io.File;

public class RunWar implements Process {
    private final Controller controller;
    private String contextPath;

    public RunWar(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void exec() throws Exception {
        if (!controller.isMaster) return;

        contextPath = "/console"; // TODO 需要可配置
        File console = FileUtil.newFile(ConsoleWarHelper.getLibDir(), "sysapp", "console");
        String docBase = console.getAbsolutePath();
        controller.servletService.addWebapp(contextPath, docBase);

        ConsoleWarHelper.getLogger().info("Open a browser to access the QingZhou console: http://localhost:9060" + contextPath);// todo 9060 应该动态获取到
    }

    @Override
    public void undo() {
        if (!controller.isMaster) return;

        controller.servletService.removeApp(contextPath);
    }
}
