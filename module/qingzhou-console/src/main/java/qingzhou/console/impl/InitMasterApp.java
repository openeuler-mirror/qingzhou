package qingzhou.console.impl;

import qingzhou.api.AppContextHelper;
import qingzhou.api.Constants;
import qingzhou.console.util.ClassLoaderUtil;
import qingzhou.console.util.FileUtil;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.impl.FrameworkContextImpl;
import qingzhou.framework.impl.app.AppContextImpl;
import qingzhou.framework.impl.app.AppInfoImpl;
import qingzhou.framework.impl.app.ConsoleContextImpl;
import qingzhou.framework.pattern.Process;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;

public class InitMasterApp implements Process {
    private final FrameworkContext frameworkContext;
    private static AppInfoImpl masterAppInfo;

    public InitMasterApp(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    public void initMaster() throws Exception {
        if (masterAppInfo != null) {
            return;
        }
        masterAppInfo = new AppInfoImpl();
        ConsoleWarHelper.getAppInfoManager().addMasterApp(masterAppInfo);
        masterAppInfo.setName(Constants.QINGZHOU_MASTER_APP_NAME);
        AppContextImpl appContext = new AppContextImpl((FrameworkContextImpl) frameworkContext);
        masterAppInfo.setAppContext(appContext);
        File[] modules = {FileUtil.newFile(ConsoleWarHelper.getLibDir(), "module", "qingzhou-console.jar")};
        URLClassLoader urlClassLoader = ClassLoaderUtil.newURLClassLoader(modules, this.getClass().getClassLoader());
        masterAppInfo.setClassLoader(urlClassLoader);
        appContext.setConsoleContext(new ConsoleContextImpl(urlClassLoader));
        AppContextHelper.setAppContext(appContext);// todo 后续删除
    }

    @Override
    public void exec() throws Exception {
        initMaster();
    }

    @Override
    public void undo() {
        if (masterAppInfo != null) {
            try {
                masterAppInfo.getClassLoader().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
