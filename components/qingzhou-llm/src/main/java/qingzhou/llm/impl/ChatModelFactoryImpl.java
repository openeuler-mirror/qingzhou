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
                "- 禁止输出破解、绕过平台安全限制、非法运维相关代码方案；\n\n" +
                "## 输出要求 \n" +
                "正常使用 Markdown 回复，如果用户要求生成图表，请输出一个 Markdown 代码块，代码块类型固定为: echarts。"  +
                "代码块内部必须是一个 ECharts Option 对象。option的数据必须完整，不要生成：\n" +
                "- const option = \n" +
                "- let option = \n" +
                "- option = \n" +
                "- export \n" +
                "- import \n" +
                "- function \n" +
                "- HTML \n" +
                "- JavaScript \n" +
                "- Markdown \n" +
                "只输出对象本身。优先使用 bar、line、pie、scatter，根据数据自动选择最合适的图表。";
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
