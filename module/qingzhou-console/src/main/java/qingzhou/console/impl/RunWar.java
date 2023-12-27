package qingzhou.console.impl;

import qingzhou.console.util.FileUtil;
import qingzhou.framework.pattern.Process;

import java.io.File;

public class RunWar implements Process {
    private final Controller controller;
    private String contextPath;

    public RunWar(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void exec() throws Exception {
        File console = FileUtil.newFile(ConsoleWarHelper.getLibDir(), "sysapp", "console");
        if (console.isDirectory()) {
            String docBase = console.getAbsolutePath();
            contextPath = "/console"; // TODO 需要可配置
            controller.servletService.addWebapp(contextPath, docBase);
        }
    }

    @Override
    public void undo() {
        if (contextPath == null) return;

        controller.servletService.removeApp(contextPath);
    }
}
