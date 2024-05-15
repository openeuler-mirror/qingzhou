package qingzhou.console.i18n;

import qingzhou.api.Lang;
import qingzhou.deployer.I18nTool;

public class ConsoleI18n {
    private static final I18nTool i18nTool = new I18nTool();

    static {
        addI18n("batch.ops.success", new String[]{"%s%s成功%s个", "en:%s %s Success %s"});
        addI18n("batch.ops.fail", new String[]{"%s%s成功%s个，失败%s个，失败详情：", "en:%s%s success %s, failure %s, failure details:"});// 一些 filter 需要 i18n，如 LoginFreeFilter 调用了Helper.convertCommonMsg(msg)，此时 RestController 等类可能都还没有初始化（例如 Rest 直连登录），会导致 i18n 信息丢失，因此放到这里
        addI18n("validator.ActionShow.notsupported", new String[]{"不支持%s操作，未满足条件：%s", "en:The %s operation is not supported, the condition is not met: %s"});// The product uses untrusted data to generated HTML pages. 客户端传来的参数不能回显到页面上，以阻止可能的xss攻击

        addI18n("client.trusted.not", new String[]{"该操作仅限于在服务器本机或受信任的IP上执行，受信任IP的设置方式请参考产品手册", "en:This operation can only be performed on the local server or on a trusted IP. Please refer to the product manual for the setting method of the trusted IP"});
    }

    public static void addI18n(String key, String[] i18n) {
        i18nTool.addI18n(key, i18n, true);
    }

    public static String getI18n(Lang lang, String key, Object... args) {
        return i18nTool.getI18n(lang, key, args);
    }

    private ConsoleI18n() {
    }
}
