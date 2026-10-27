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
        // 勿再加 issuer 等参数：URI 每多约 14 个字符，ECC HIGH 下二维码版本就升一档，终端宽度可能不够
        String uri = "otpauth://totp/qingzhou?secret=" + secret;
        if (isQrRenderable()) {
            init.log(new QrCodeImpl().genText(uri));
        } else {
            init.log("QR code omitted: this terminal needs UTF-8 with ANSI support.");
        }
        init.log("Manual entry: " + uri);
        // 先让用户用验证器扫码并回填口令：密钥只显示这一次，配错将导致管理端无法登录
        if (!confirm(init, secret)) {
            init.log("TOTP code verification failed, skipped.");
            return;
        }

        props = Init.putValue(props, "qingzhou-auth.totp_secret",
                init.getCrypto().getGlobalCipher().encrypt(secret));
        props = Init.putValue(props, "qingzhou-auth.totp_enabled", "true");
        init.writeProps(props);
        init.log("TOTP two-factor login enabled.");
    }

    // 半块字符要求终端编码能表示这些字形，ANSI 取色要求终端解释 VT 序列，缺一即乱码
    private static boolean isQrRenderable() {
        if (!canEncodeHalfBlocks()) return false;
        return !Processor.IS_WINDOWS || hasVtTerminal();
    }

    // 判的是编码能力而非编码名：UTF-8 之外也有含半块字形的字符集，无法表示的字符会被静默替换成 '?'
    private static boolean canEncodeHalfBlocks() {
        String encoding = System.getProperty("sun.stdout.encoding");
        if (encoding == null) encoding = Charset.defaultCharset().name();
        try {
            return Charset.forName(encoding).newEncoder().canEncode("▀▄█");
        } catch (IllegalArgumentException e) { // 属性可能是非标准字符集名，判不出时按不可渲染处理
            return false;
        }
    }

    // JDK8 不会为 Windows 控制台开启 VT：只能凭环境变量识别支持 VT 的终端，判不出时降级
    private static boolean hasVtTerminal() {
        return System.getenv("WT_SESSION") != null || System.getenv("TERM") != null;
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
