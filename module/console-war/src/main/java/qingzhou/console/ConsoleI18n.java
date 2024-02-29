package qingzhou.console;

import qingzhou.api.Lang;
import qingzhou.framework.app.I18NStore;

public class ConsoleI18n {
    private static final I18NStore i18NStore = new I18NStore();

    static {
        addI18N("batch.ops.success", new String[]{"%s%s成功%s个", "en:%s %s Success %s"});
        addI18N("batch.ops.fail", new String[]{"%s%s成功%s个，失败%s个，失败详情：", "en:%s%s success %s, failure %s, failure details:"});// 一些 filter 需要 i18n，如 LoginFreeFilter 调用了Helper.convertCommonMsg(msg)，此时 RestController 等类可能都还没有初始化（例如 Rest 直连登录），会导致 i18n 信息丢失，因此放到这里
        addI18N("validator.ActionEffective.notsupported", new String[]{"不支持%s操作，未满足条件：%s", "en:The %s operation is not supported, the condition is not met: %s"});// The product uses untrusted data to generated HTML pages. 客户端传来的参数不能回显到页面上，以阻止可能的xss攻击

        addI18N("client.trusted.not", new String[]{"该操作仅限于在服务器本机或受信任的IP上执行，受信任IP的设置方式请参考产品手册", "en:This operation can only be performed on the local server or on a trusted IP. Please refer to the product manual for the setting method of the trusted IP"});
    }

    public static void addI18N(String key, String[] i18n) {
        i18NStore.addI18N(key, i18n, true);
    }

    public static String getI18N(Lang lang, String key, Object... args) {
        return i18NStore.getI18N(lang, key, args);
    }

    private ConsoleI18n() {
    }
}
