package qingzhou.command.cmd;

import java.io.Console;
import java.io.File;
import java.util.Arrays;

import qingzhou.command.Processor;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;

/**
 * 用实例全局密钥加密明文口令：读取后不回显，仅向 stdout 输出密文，便于直接写入配置文件。
 * 全局密钥为本机信任根，密钥文件权限须保持属主私有（见 Init.restrictToOwnerOnly）。
 */
public class Encrypt extends Processor {
    @Override
    public String name() {
        return "encrypt";
    }

    @Override
    public String info() {
        return "Encrypt a plaintext password with the instance global key. Usage: encrypt <instance>";
    }

    @Override
    public String[] supportedArgs() {
        return new String[]{"<instance>"};
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        File instanceDir = initInstance(args); // 校验实例存在与名称安全（防目录穿越）
        if (instanceDir == null) return;

        Crypto crypto = createCrypto(instanceDir, globalCipherKeyName(instanceDir));

        Cipher cipher;
        try {
            cipher = crypto.getGlobalCipher(); // 缺密钥文件或对应密钥属性时抛 IllegalStateException
        } catch (Exception e) {
            log("failed to load global key: " + e.getMessage());
            return;
        }

        char[] plaintext = readPlaintext();
        if (plaintext == null) return; // EOF/Ctrl-D
        try {
            log(cipher.encrypt(new String(plaintext)));
        } finally {
            Arrays.fill(plaintext, '\0'); // 口令不驻留内存
        }
    }

    private char[] readPlaintext() {
        Console console = System.console();
        if (console == null) { // 确认与密码均需交互，无终端时不得按默认值擅自初始化
            log("no interactive terminal found, run this command in a terminal");
            return null;
        } else {
            return console.readPassword("Password: ");
        }
    }
}
