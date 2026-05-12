package qingzhou.registry.service.llm;

import java.io.File;
import java.util.function.Function;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import qingzhou.llm.Parameter;
import qingzhou.llm.Tool;

@Component
public class KnowledgeTool extends BaseLlmTool implements Tool {

    private KnowledgeIndex knowledgeIndex;

    @Activate
    public void init() {
        knowledgeIndex = new KnowledgeIndex();
        // user.dir 运行时指向 frame/instances/{instance}/，其父目录的父目录即为 qingzhou.home
        String qingzhouHome = new File(System.getProperty("user.dir")).getParentFile().getParent();
        String docsPath = qingzhouHome + File.separator + "docs";
        knowledgeIndex.loadFromDirectory(docsPath);
    }

    @Override
    public String description() {
        return "该接口用于搜索轻舟平台的文档知识库，返回与查询关键词相关的文档内容。"
                + "知识库包含：项目概述与架构说明、应用开发规范、单元测试开发规范、"
                + "Nginx应用使用说明、版本更新记录、故障排除文档等。"
                + "当用户询问轻舟平台的使用方法、开发规范、功能说明、配置方式、架构设计等问题时，"
                + "应调用此接口获取相关文档内容来回答。";
    }

    @Override
    public Parameter[] parameters() {
        return new Parameter[]{
                Parameter.of("query", "搜索关键词，用于在文档知识库中查找相关内容。"
                        + "可以是主题名称（如'开发规范'）、功能关键词（如'nginx'、'配置'）"
                        + "或问题关键词（如'部署'、'测试'）"),
                Parameter.of("list_only", "是否仅列出可用的文档主题列表而不返回具体内容，默认为false", false, null)
        };
    }

    private final Function<HandlingContext, Object> function = (context) -> {
        String listOnly = context.getParameter("list_only");

        if ("true".equalsIgnoreCase(listOnly)) {
            return knowledgeIndex.listTopics();
        }

        String query = context.getParameter("query");
        return knowledgeIndex.search(query, 5);
    };

    @Override
    protected Function<HandlingContext, Object> toolHandler() {
        return function;
    }
}
