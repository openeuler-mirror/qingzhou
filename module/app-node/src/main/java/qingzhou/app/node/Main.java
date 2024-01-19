package qingzhou.app.node;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.impl.FrameworkContextImpl;

public class Main extends QingZhouApp { // todo： 节点支持“管理”，可查看节点的状态信息
    @Override
    public void start(AppContext appContext) {
    }

    public static FrameworkContext getFC() {
        return FrameworkContextImpl.getFrameworkContext();
    }
}
