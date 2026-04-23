package qingzhou.app.demo;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Menu;
import qingzhou.api.QingzhouApp;

@App(code = "demo", icon = "Promotion",
        name = {"示例应用", "en:Demo Application"},
        info = {"用于演示轻舟的功能。", "en:Used to demo the ability of Qingzhou."})
@Menu(name = {"基础功能", "en:Basic"}, code = "basic", icon = "cog", order = 1)
@Menu(name = {"高级功能", "en:Advanced"}, code = "advanced", icon = "tools", order = 2)
@Menu(name = {"系统", "en:System"}, code = "system", icon = "setting", order = 3)
public class DemoApp implements QingzhouApp {
    @Override
    public void start(AppContext appContext) {
    }
}
