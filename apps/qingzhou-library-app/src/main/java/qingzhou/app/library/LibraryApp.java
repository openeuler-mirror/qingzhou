package qingzhou.app.library;

import qingzhou.api.App;
import qingzhou.api.AppContext;
import qingzhou.api.Menu;
import qingzhou.api.QingzhouApp;

@App(icon = "ReadingFilled",
        name = {"图书管理", "en:Book Management"},
        info = {"完整的图书馆业务管理系统，包含图书、读者、借阅等核心功能。", "en:Complete library management system with books, readers, and borrow records."})
@Menu(name = {"基础管理", "en:Basic Management"}, code = "basic", icon = "Grid", order = 1)
@Menu(name = {"借阅管理", "en:Borrow Management"}, code = "borrow", icon = "Files", order = 2)
public class LibraryApp implements QingzhouApp {
    @Override
    public void start(AppContext appContext) {
    }
}
