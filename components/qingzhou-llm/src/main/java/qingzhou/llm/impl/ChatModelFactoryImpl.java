package qingzhou.llm.impl;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.noear.solon.ai.chat.dialect.ChatDialectManager;
import org.noear.solon.ai.llm.dialect.openai.OpenaiChatDialect;
import org.noear.solon.ai.llm.dialect.openai.OpenaiResponsesDialect;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.llm.*;
import qingzhou.llm.impl.log.Slf4jLogBridge;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-llm-chat", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ChatModelFactoryImpl implements ChatModelFactory {
    @Reference
    private Logger logger;

    private org.noear.solon.ai.chat.ChatModel chatModel;

    @Activate
    public void init(Map<String, String> config) {
        // 放在 solon 加载最前面
        Slf4jLogBridge.qingzhouLogger = logger;

        // chat 模型需要
        ChatDialectManager.register(new OpenaiChatDialect());
        ChatDialectManager.register(new OpenaiResponsesDialect());

        String systemPrompt = "## 禁止事项：\n" +
                "- 不得编造 Qingzhou 未提及的功能、接口或配置项；\n" +
                "- 不得对平台安全性、性能等做出未经验证的承诺性描述；\n" +
                "- 禁止恶意贬低 / 夸大产品能力、跨产品踩一捧一；\n" +
                "- 禁止输出破解、绕过平台安全限制、非法运维相关代码方案；\n";
        chatModel = org.noear.solon.ai.chat.ChatModel
                .of(config.get("base_url"))
                .apiKey(config.get("api_key"))
                .model(config.get("model"))
                // 设置超时，防止无限等待
                .timeout(Duration.ofSeconds(Long.parseLong(config.getOrDefault("timeout", "60"))))
                // Anthropic Claude 专有参数：开启思考
                .modelOptions(op -> op.optionSet("thinking", new HashMap<String, Object>() {{
                    put("type", "adaptive");
                }}))
                .systemPrompt(systemPrompt)
                .build();
    }

    @Override
    public ChatModel.Builder newChatModelBuilder() {
        return new ChatModel.Builder() {
            private List<String> docs;
            private Collection<Tool> tools;
            private Collection<Skill> skills;

            @Override
            public ChatModel.Builder withDoc(List<String> docs) {
                this.docs = docs;
                return this;
            }

            @Override
            public ChatModel.Builder withTool(Collection<Tool> tools) {
                this.tools = tools;
                return this;
            }

            @Override
            public ChatModel.Builder withSkill(Collection<Skill> skills) {
                this.skills = skills;
                return this;
            }

            @Override
            public ChatModel build() {
                return new ChatModelImpl(chatModel, docs, tools, skills);
            }
        };
    }

    @Override
    public Attachment buildImageAttachment(String base64) {
        return new ImageAttachment(base64);
    }
}
