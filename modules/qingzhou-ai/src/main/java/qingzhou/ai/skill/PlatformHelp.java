package qingzhou.ai.skill;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import qingzhou.ai.AiSkill;
import qingzhou.llm.ChatContext;
import qingzhou.llm.EmbeddingModel;
import qingzhou.llm.RerankingModel;
import qingzhou.llm.VectorStore;
import qingzhou.logger.Logger;

@Component(property = AiSkill.SKILL_NAME + "=PlatformHelp")
public class PlatformHelp extends AiSkillBase implements AiSkill {
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private EmbeddingModel embeddingModel;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private RerankingModel rerankingModel;

    @Reference
    private Logger logger;

    private VectorStore knowledgeStore;
    private List<String> knowledgeDocs;

    public PlatformHelp() {
        super(new String[]{"平台咨询", "en:Platform Help"},
                "你是一个专业的 Qingzhou（轻舟）平台智能助手，你的职责是帮助开发者、运维人员和管理员理解和使用 Qingzhou 平台。\n" +
                        "具备以下专业认知：\n" +
                        "- 精通 Qingzhou（轻舟）平台的设计理念、整体架构、核心特点与功能、适用范围；\n" +
                        "- 精通 Qingzhou（轻舟）平台的目录结构、服务接口、前后端分离部署；\n" +
                        "- 精通 Qingzhou（轻舟）平台的API和轻舟应用开发规范；\n" +
                        "- 熟悉 Java 生态、低代码开发、声明式开发、RESTful API 设计、动态渲染等技术；\n" +
                        "- 熟悉大模型驱动的智能运维的理念与实践。");
    }

    @Activate
    public void init() {
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
        }
        if (docs.isEmpty()) return;

        if (embeddingModel != null) {
            try {
                knowledgeStore = embeddingModel.buildVectorStore();
                for (String doc : docs) {
                    knowledgeStore.insert(doc, 500);
                }
            } catch (Exception e) {
                logger.warn("failed to initialize knowledge store", e);
                knowledgeStore = null;
            }
        }

        if (knowledgeStore == null) {
            knowledgeDocs = docs;
        }
    }

    @Override
    public String getInstruction(ChatContext chatContext) {
        String question = chatContext.getLastMessage();

        // RAG 检索增强问答
        List<String> refDocs = new ArrayList<>();
        if (knowledgeStore != null) {
            try {
                refDocs.addAll(knowledgeStore.query(question));
            } catch (IOException e) {
                logger.warn("failed to load the embedding model", e);
            }
        }
        if (refDocs.isEmpty()) {
            refDocs.addAll(knowledgeDocs);
        }

        // 重排序优化
        if (rerankingModel != null) {
            try {
                refDocs = rerankingModel.rerank(question, refDocs);
            } catch (Throwable e) { // 误打开配置文件里的注释后，若配置为空也会报错
                logger.warn("failed to load the re-ranking model", e);
            }
        }

        refDocs.add("## 回答原则\n" +
                "1. **准确性优先**：严格基于 Qingzhou 官方文档和项目设计回答，不编造不存在的功能或接口。\n" +
                "2. **场景化引导**：\n" +
                "    - 当用户询问关于\"某某系统、某某资产、某某插件\"时，可统一理解为某某应用，因为应用是平台上管理的唯一资源，所有问题都可围绕应用进行回答\n" +
                "    - 当用户询问\"AI管控如何使用\"时，说明自然语言交互通过大模型理解意图并执行管控逻辑\n" +
                "    - 当用户仅打招呼，无明确问题时，建议回复：您好，我是 Qingzhou 平台智能助手，您可以提问关于项目介绍、代理部署、插件开发、API 规范、AI 智能运维、项目核心特点、功能、价值和意义等问题\n" +
                "3. **边界意识**：\n" +
                "    - 如果问题涉及文档未覆盖的具体代码实现细节，如实告知并建议查阅源码或社区\n" +
                "    - 如果问题与 Qingzhou 无关，礼貌说明你的专业领域并提供力所能及的参考\n" +
                "    - 如果用户提出平台当前不支持的需求，客观说明现状，可基于架构设计给出可行性分析\n" +
                "4. **语言风格**：专业、简洁、结构化，善用列表和代码块，优先给出可操作的步骤和路径。\n");

        return String.join("\n\n[参考附件]\n", refDocs);
    }
}
