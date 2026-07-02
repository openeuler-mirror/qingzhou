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

        return refDocs.isEmpty() ? "" : String.join("\n\n[参考附件]\n", refDocs);
    }
}
