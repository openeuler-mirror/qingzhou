package qingzhou.command.cmd.init;

import java.nio.charset.Charset;

import qingzhou.command.Processor;
import qingzhou.image.impl.QrCodeImpl;

final class TotpStep {
    void init(Init init) throws Exception {
        String props = init.readProps();
        if (Init.hasValue(props, "qingzhou-auth.totp_secret")) return; // 已有值则静默跳过

        if (init.noConfirm("Enable TOTP two-factor login? (optional, needs an authenticator app) [y/N] ")) return;

        String secret = init.getCrypto().getTotp().generateKey();
        String uri = "otpauth://totp/qingzhou?secret=" + secret + "&issuer=qingzhou";
        if (isQrRenderable()) {
            init.log(new QrCodeImpl().genText(uri));
        } else {
            init.log("QR code omitted: it needs a UTF-8 terminal with ANSI support; use the secret below.");
        }
        init.log("Secret (base32, shown only once): " + secret);
        init.log(uri);
        // 先让用户用验证器扫码并回填口令：密钥只显示这一次，配错将导致管理端无法登录
        if (!confirm(init, secret)) {
            init.log("TOTP code verification failed, skipped.");
            return;
        }

        props = Init.putValue(props, "qingzhou-auth.totp_secret", init.getCrypto().getGlobalCipher().encrypt(secret));
        props = Init.putValue(props, "qingzhou-auth.totp_enabled", "true");
        init.writeProps(props);
        init.log("TOTP two-factor login enabled.");
    }

    // Windows 传统控制台未开启 VT 且常为 GBK 代码页，半块字符与 ANSI 转义会显示成乱码
    private boolean isQrRenderable() {
        if (Processor.IS_WINDOWS) return false;
        String encoding = System.getProperty("sun.stdout.encoding");
        if (encoding == null) encoding = Charset.defaultCharset().name();
        return encoding.replace("-", "").equalsIgnoreCase("utf8");
    }

    // 按 TOTP 的 30 秒窗口，三次足够覆盖用户输入期间的换窗
    private static boolean confirm(Init init, String secret) throws Exception {
        for (int i = 0; i < 3; i++) {
            String code = init.readLine("Code from the authenticator app: ");
            if (code != null && init.getCrypto().getTotp().verifyCodeWithReplayGuard(secret, code.trim())) return true;
        }
        return false;
    }
}
