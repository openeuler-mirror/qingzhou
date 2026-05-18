package qingzhou.app.demo;

import java.io.File;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Menu;
import qingzhou.api.QingzhouApp;
import qingzhou.logger.Logger;

@App(icon = "Promotion",
        name = {"示例应用示例应用示例应用示例应用", "en:Demo Application"},
        info = {"用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。用于演示轻舟的功能。", "en:Used to demo the ability of Qingzhou."})
@Menu(name = {"基础功能", "en:Basic"}, code = "basic", icon = "cog", order = 1)
@Menu(name = {"高级功能", "en:Advanced"}, code = "advanced", icon = "tools", order = 2)
@Menu(name = {"子菜单一", "en:Sub Menu 1"}, code = "sub1", parent = "advanced", icon = "document", order = 1)
@Menu(name = {"子菜单二", "en:Sub Menu 2"}, code = "sub2", parent = "advanced", icon = "folder", order = 2)
@Menu(name = {"三级菜单示例", "en:Level 3 Menu"}, code = "level3", parent = "sub2", icon = "setting", order = 1)
@Menu(name = {"监视", "en:Monitor"}, code = "monitor", icon = "Monitor", order = 3)
@Menu(name = {"系统", "en:System"}, code = "system", icon = "setting", order = 4)
public class DemoApp implements QingzhouApp {
    @Override
    public boolean available(AppContext appContext) {
        return !new File(appContext.getTemp(), "stop").exists();
    }

    @Override
    public void start(AppContext appContext) {
        Logger logger = appContext.getService(Logger.class);
        logger.info("Demo 应用启动成功！");
    }
}
