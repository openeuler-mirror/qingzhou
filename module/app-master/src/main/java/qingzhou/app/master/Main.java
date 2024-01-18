package qingzhou.app.master;

import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.QingZhouApp;
import qingzhou.framework.impl.FrameworkContextImpl;
import qingzhou.framework.util.FileUtil;

import java.io.File;

public class Main extends QingZhouApp {
    @Override
    public void start(AppContext appContext) {
        ConsoleContext consoleContext = appContext.getConsoleContext();
        consoleContext.setMenuInfo("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
        consoleContext.setMenuInfo("Security", new String[]{"安全管理", "en:Security"}, "shield", 2);
        consoleContext.setMenuInfo("System", new String[]{"系统管理", "en:System"}, "cog", 3);

        // 一些 filter 需要 i18n，如 LoginFreeFilter 调用了Helper.convertCommonMsg(msg)，此时 RestController 等类可能都还没有初始化（例如 Rest 直连登录），会导致 i18n 信息丢失，因此放到这里
        consoleContext.addI18N("validator.ActionEffective.notsupported", new String[]{"不支持%s操作，未满足条件：%s", "en:The %s operation is not supported, the condition is not met: %s"});// The product uses untrusted data to generated HTML pages. 客户端传来的参数不能回显到页面上，以阻止可能的xss攻击
        consoleContext.addI18N("batch.ops.success", new String[]{"%s%s成功%s个", "en:%s %s Success %s"});
        consoleContext.addI18N("batch.ops.fail", new String[]{"%s%s成功%s个，失败%s个，失败详情：", "en:%s%s success %s, failure %s, failure details:"});
        File serverXml = FileUtil.newFile(getFC().getDomain(), "conf", "server.xml");
        appContext.setDataStore(new DataStoreImpl(serverXml));
    }

    public static FrameworkContext getFC() {
        return FrameworkContextImpl.getFrameworkContext(); // todo 没有报找不到？
    }
}
