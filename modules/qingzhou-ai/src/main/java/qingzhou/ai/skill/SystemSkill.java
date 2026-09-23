package qingzhou.ai.skill;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = SkillService.SKILL_NAME + "=" + SkillService.SYSTEM_SKILL)
public class SystemSkill extends SkillServiceBase implements SkillService {
    @Reference
    private Logger logger;

    private List<String> knowledgeDocs;

    public SystemSkill() {
        super("当用户意图涉及理解和使用轻舟平台时激活此技能。具体激活场景包括但不限于：\n" +
                "- 平台的设计理念、整体架构、核心特点与功能、适用范围；\n" +
                "- 平台的目录结构、服务接口、前后端分离部署；\n" +
                "- 平台的API和轻舟应用开发规范；");
    }

    @Activate
    public void init(Map<String, String> config) {
        List<String> docs = new ArrayList<>();
        String instance = config.get("qingzhou.instance");
        String version = config.get("qingzhou.version");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                Paths.get(new File(instance).getParentFile().getParent(), "lib", "version" + version, "docs"),
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

        knowledgeDocs = docs;
    }

    @Override
    public String instruction() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 Qingzhou（轻舟）平台智能助手，帮助开发者、运维人员和管理员理解和使用 Qingzhou 平台。\n");
        sb.append("精通 Qingzhou 的整体架构、核心特性和设计理念，精通 Java 生态、低代码开发、声明式开发、RESTful API 设计、动态渲染。\n");
        sb.append("当用户询问\"AI管控如何使用\"时，说明自然语言交互通过大模型理解意图并执行管控逻辑。\n");
        sb.append("当用户询问关于\"某某系统、某某资产、某某插件\"时，统一理解为某某应用，因为应用是平台上管理的唯一资源。\n");
        if (knowledgeDocs != null && !knowledgeDocs.isEmpty()) {
            sb.append("\n[参考附件]\n");
            sb.append(String.join("\n\n[参考附件]\n", knowledgeDocs));
        }
        return sb.toString();
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + SkillService.SKILL_NAME + "=" + SkillService.SYSTEM_SKILL + ")", // 按服务属性过滤
            unbind = "unbindAiTool")
    public void bindAiTool(ToolService tool, Map<String, Object> properties) {
        aiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindAiTool(ToolService tool) {
        aiTools.remove(tool);
    }
}
