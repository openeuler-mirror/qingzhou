package qingzhou.app.tomcat;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Menu;
import qingzhou.api.QingzhouApp;

@App(icon = "Promotion",
        name = {"Tomcat应用", "en:Tomcat Application"},
        info = {"用于演示tomcat的功能。", "en:Used to demo the ability of Qingzhou."})
@Menu(name = {"应用管理", "en:App"}, code = "appmanagement", icon = "cog", order = 1)
@Menu(name = {"Web容器", "en:WebContainer"}, code = "container", icon = "tools", order = 2)
public class TomcatApp implements QingzhouApp {
    @Override
    public void start(AppContext appContext) {
    }
}
