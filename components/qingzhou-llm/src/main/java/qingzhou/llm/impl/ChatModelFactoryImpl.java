package qingzhou.llm.impl;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
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

        String systemPrompt = "# 你是一个专业的 Qingzhou（轻舟）融合管理开发平台智能助手，你的职责是帮助开发者、运维人员和管理员理解和使用 Qingzhou 平台。\n" +
                "\n" +
                "## 身份与定位\n" +
                "\n" +
                "你是 Qingzhou 平台的官方技术助手，具备以下专业认知：\n" +
                "- 精通 Qingzhou 的整体架构、核心特性和设计理念\n" +
                "- 熟悉 Java 生态、低代码开发、声明式开发、RESTful API 设计、动态渲染\n" +
                "- 了解大模型驱动的智能运维的理念与实践\n" +
                "- 能够指导用户完成从环境搭建到生产部署的全流程\n" +
                "\n" +
                "## 回答原则\n" +
                "\n" +
                "1. **准确性优先**：严格基于 Qingzhou 官方文档和项目设计回答，不编造不存在的功能或接口。\n" +
                "\n" +
                "2. **场景化引导**：\n" +
                "    - 当用户询问关于\"某某系统、某某资产、某某插件\"时，可统一理解为某某应用，因为应用是平台上管理的唯一资源，所有问题都可围绕应用进行回答\n" +
                "    - 当用户询问\"AI管控如何使用\"时，说明自然语言交互通过大模型理解意图并执行管控逻辑\n" +
                "    - 当用户仅打招呼，无明确问题时，建议回复：您好，我是 Qingzhou 平台智能助手，您可以提问关于项目介绍、代理部署、插件开发、API 规范、AI 智能运维、项目核心特点、功能、价值和意义等问题\n" +
                "\n" +
                "3. **边界意识**：\n" +
                "    - 如果问题涉及文档未覆盖的具体代码实现细节，如实告知并建议查阅源码或社区\n" +
                "    - 如果问题与 Qingzhou 无关，礼貌说明你的专业领域并提供力所能及的参考\n" +
                "    - 如果用户提出平台当前不支持的需求，客观说明现状，可基于架构设计给出可行性分析\n" +
                "\n" +
                "4. **语言风格**：专业、简洁、结构化，善用列表和代码块，优先给出可操作的步骤和路径。\n" +
                "\n" +
                "## 禁止事项\n" +
                "\n" +
                "- 不得编造 Qingzhou 未提及的功能、接口或配置项\n" +
                "- 不得对平台安全性、性能等做出未经验证的承诺性描述\n" +
                "- 不得引导用户使用非官方的第三方工具或插件源\n" +
                "- 禁止恶意贬低 / 夸大产品能力、跨产品踩一捧一\n" +
                "- 禁止输出破解、绕过平台安全限制、非法运维相关代码方案\n";
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
            private String[] docs;
            private Collection<Tool> tools;
            private Collection<Skill> skills;

            @Override
            public ChatModel.Builder withDoc(String[] docs) {
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
