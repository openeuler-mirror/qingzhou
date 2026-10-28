package qingzhou.command.cmd.init;

import java.io.File;

final class AgentDomainStep {
    // 键格式 <prefix><domain> 须与 AgentDomainServiceImpl 的解析保持一致
    private static final String KEY_PREFIX = "agent-domain~";

    void init(Init init) throws Exception {
        File secretFile = init.confFile("secret-key.properties");
        String content = Init.read(secretFile);
        while (init.confirm("Add an agent domain? (agent and registry share this key) [y/N] ")) {
            String domain = init.readLine("Agent domain name: ");
            if (domain == null) return; // 终端已关闭，结束交互
            domain = domain.trim();
            if (!isValidDomain(domain)) {
                init.log("Invalid agent domain name: letters, digits, '-' and '_' only, 1-64 chars.");
                continue;
            }

            String keyName = KEY_PREFIX + domain;
            if (Init.hasValue(content, keyName)) { // 已有密钥即信任根已建立，重写等于轮换，只能由人显式改文件
                init.log("Agent domain already exists: " + domain);
                continue;
            }

            String value = init.getCrypto().generateSecureKey();
            content = Init.putValue(content, keyName, value);
            Init.write(secretFile, content);
            Init.restrictToOwnerOnly(secretFile);
            // 域密钥须带外分发到同域 agent，此处完整回显，省去用户回读密钥文件
            init.log("Agent domain added for: " + keyName);
        }
    }

    // 域名会拼进 properties 键名与 AAD 上下文：限定字符集，杜绝 = : # 空格等破坏配置解析
    private static boolean isValidDomain(String domain) {
        if (domain.isEmpty() || domain.length() > 64) return false;
        for (int i = 0; i < domain.length(); i++) {
            char c = domain.charAt(i);
            boolean isAllowed = c >= 'a' && c <= 'z'
                    || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9'
                    || c == '-'
                    || c == '_';
            if (!isAllowed) return false;
        }
        return true;
    }
}
