package qingzhou.ai.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.*;
import qingzhou.ai.AiTool;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.*;
import qingzhou.logger.Logger;

@Component(property = HttpHandler.HANDLE_PATH + "=/chat",
        configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ChatHttpHandler implements HttpHandler {
    @Reference
    private LLM llm;
    @Reference
    private Logger logger;
    @Reference
    private Json json;

    private ChatModel chatModel;
    private final String systemPrompt = "# 你是一个专业的 Qingzhou（轻舟）融合管理开发平台智能助手，你的职责是帮助开发者、运维人员和管理员理解、使用、部署和扩展 Qingzhou 平台。\n" +
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
            "   - 当用户询问\"如何部署\"时，引导其关注 JDK 1.8+ 等环境准备，以及 bin/start.sh 启动流程\n" +
            "   - 当用户询问\"如何开发插件\"时，强调遵循轻舟API规范，参考插件化架构设计\n" +
            "   - 当用户询问\"AI管控如何使用\"时，说明自然语言交互通过大模型理解意图并执行管控逻辑\n" +
            "\n" +
            "3. **架构解释能力**：能够清晰解释\"轻舟代理\"与\"轻舟管控台\"的关系、\"本地实例\"与\"远程实例\"的通信机制、以及\"应用层-服务层-驱动层\"的分层架构。\n" +
            "\n" +
            "4. **边界意识**：\n" +
            "   - 如果问题涉及文档未覆盖的具体代码实现细节，如实告知并建议查阅源码或社区\n" +
            "   - 如果问题与 Qingzhou 无关，礼貌说明你的专业领域并提供力所能及的参考\n" +
            "   - 如果用户提出平台当前不支持的需求，客观说明现状，可基于架构设计给出可行性分析\n" +
            "\n" +
            "5. **语言风格**：专业、简洁、结构化，善用列表和代码块，优先给出可操作的步骤和路径。\n" +
            "\n" +
            "## 禁止事项\n" +
            "\n" +
            "- 不得编造 Qingzhou 未提及的功能、接口或配置项\n" +
            "- 不得对平台安全性、性能等做出未经验证的承诺性描述\n" +
            "- 不得引导用户使用非官方的第三方工具或插件源\n" +
            "- 禁止恶意贬低 / 夸大产品能力、跨产品踩一捧一\n" +
            "- 禁止输出破解、绕过平台安全限制、非法运维相关代码方案\n" +
            "\n" +
            "## 快捷引导话术（用户无明确问题时）\n" +
            "\n" +
            "用户仅打招呼 / 无提问：「您好，我是轻舟 Qingzhou 平台智能助手，您可以咨询：项目介绍、代理部署、插件开发、API 规范、AI 智能运维、项目核心特点、功能、价值和意义等相关问题」。\n";
    private VectorStore knowledgeStore;
    private String[] knowledgeDocs;
    private final Map<AiTool, Map<String, Object>> aiTools = new HashMap<>();
    private RerankingModel rerankingModel;


    @Activate
    public void init(Map<String, String> config) {
        // rag 检索
        Object knowledge = initKnowledge(config);
        if (knowledge instanceof VectorStore) {
            knowledgeStore = (VectorStore) knowledge;
        } else if (knowledge instanceof String[]) {
            knowledgeDocs = (String[]) knowledge;
        } else {
            throw new IllegalStateException(String.valueOf(knowledge));
        }

        // rag 优化排序
        String rerankUrl = config.get("rerank.base_url");
        if (rerankUrl != null && !rerankUrl.isEmpty()) {
            rerankingModel = llm.buildRerankingModel(rerankUrl, config.get("rerank.api_key"),
                    config.get("rerank.model"));
        }

        // chat 大模型
        chatModel = llm.buildChatModel(
                config.get("chat.base_url"),
                config.get("chat.api_key"),
                config.get("chat.model"),
                Long.parseLong(config.getOrDefault("chat.timeout", "60")),
                systemPrompt);
    }

    private Object initKnowledge(Map<String, String> config) {
        List<String> docs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                Paths.get(System.getProperty("qingzhou.version"), "docs"),
                "*.md")) {
            for (Path md : stream) {
                List<String> contents = Files.readAllLines(md);
                if (!contents.isEmpty()) {
                    docs.add(String.join(System.lineSeparator(), contents));
                }
            }
        } catch (Exception e) {
            logger.warn("failed to read knowledge", e);
            return null;
        }
        if (docs.isEmpty()) return null;

        VectorStore knowledgeStore = null;
        String embedUrl = config.get("embed.base_url");
        if (embedUrl != null && !embedUrl.isEmpty()) {
            try {
                EmbeddingModel embeddingModel = llm.buildEmbeddingModel(embedUrl, config.get("embed.api_key"), config.get("embed.model"));
                knowledgeStore = embeddingModel.buildVectorStore();
                for (String doc : docs) {
                    knowledgeStore.insert(doc, 500);
                }
            } catch (Exception e) {
                logger.warn("failed to initialize knowledge store", e);
                knowledgeStore = null;
            }
        }

        return knowledgeStore != null ? knowledgeStore : docs.toArray(new String[0]);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindAiTool(AiTool tool, Map<String, Object> properties) {
        aiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法
    public void unbindAiTool(AiTool tool) {
        aiTools.remove(tool);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        // 解析请求
        String message = null;
        byte[] body = httpRequest.getBody();
        if (body != null && body.length > 0) {
            String str = new String(body, StandardCharsets.UTF_8);
            try {
                // 在应用里面可包含实例id和应用code等参数
                Map<String, String> map = json.fromJson(str, HashMap.class);
                message = map.entrySet().stream().filter(e -> e.getValue() != null).map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
            } catch (Exception e) {
                logger.error("failed to convert to JSON: " + str);
            }
        }
        if (message == null || message.isEmpty()) {
            httpResponse.sendFinish(resultToString(SseResult.type("RUN_ERROR").message("message cannot be null"), json));
            return;
        }

        // RAG 检索增强问答
        String[] refDocs;
        if (knowledgeStore != null) {
            refDocs = knowledgeStore.query(message);
        } else {
            refDocs = knowledgeDocs;
        }
        if (refDocs != null && rerankingModel != null) {
            refDocs = rerankingModel.rerank(message, refDocs);
        }

        // 发出响应
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache");
        chatModel.chat(message, refDocs, tools(), new SseListener(httpResponse, logger, json));
    }

    private Set<Tool> tools() {
        return aiTools.entrySet().stream().map(entry -> {
            AiTool aiTool = entry.getKey();
            Map<String, Object> toolProp = entry.getValue();
            String toolDescription = toolProp.get(AiTool.TOOL_DESCRIPTION).toString();
            String toolName;
            Object toolNameObj = toolProp.get(AiTool.TOOL_NAME);
            if (toolNameObj != null) {
                toolName = (String) toolNameObj;
            } else {
                Object componentName = toolProp.get(ComponentConstants.COMPONENT_NAME);
                if (componentName == null) {
                    throw new IllegalArgumentException("missing parameter [" + AiTool.TOOL_NAME + "] for: " + toolDescription);
                }
                String component = componentName.toString();
                int i = component.lastIndexOf(".");
                toolName = component.substring(i + 1);
            }

            return Tool.of(toolName, toolDescription, parameters(toolProp), toolArgs -> {
                try {
                    return aiTool.invoke(toolArgs);
                } catch (Exception e) {
                    throw new RuntimeException(
                            toolArgs != null ? toolArgs.toString() : e.getMessage(),
                            e);
                }
            });
        }).collect(Collectors.toSet());
    }

    private Parameter[] parameters(Map<String, Object> toolProp) {
        Map<String, Map<String, String>> params = new LinkedHashMap<>();

        toolProp.forEach((key, value) -> Stream.of(
                AiTool.PARAMETER_NAME, AiTool.PARAMETER_DESCRIPTION, AiTool.PARAMETER_REQUIRED).forEach(flag -> {
            if (key.startsWith(flag)) {
                String sp = "";
                int i = key.indexOf(".");
                if (i != -1) {
                    sp = key.substring(i);
                }
                Map<String, String> param = params.computeIfAbsent(sp, s -> new HashMap<>());
                param.put(flag, (String) value);
            }
        }));

        return params.values().stream()
                .map(map -> Parameter.of(
                        map.get(AiTool.PARAMETER_NAME),
                        map.get(AiTool.PARAMETER_DESCRIPTION),
                        Boolean.parseBoolean(map.getOrDefault(AiTool.PARAMETER_REQUIRED, "true"))))
                .toArray(Parameter[]::new);
    }

    static String resultToString(SseResult result, Json json) {
        String toJson;
        try {
            toJson = json.toJson(result.data);
        } catch (Exception e) {
            toJson = e.getMessage();
        }
        return String.format("event: %s\ndata: %s\n\n", result.type, toJson);
    }
}
