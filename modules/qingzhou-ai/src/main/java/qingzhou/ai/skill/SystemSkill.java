package qingzhou.ai.skill;

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

@Component(property = {
        SkillService.SKILL_NAME + "=" + SkillService.SYSTEM_SKILL,
        SkillService.SKILL_REQUIRED + "=true"})
public class SystemSkill extends SkillServiceBase implements SkillService {
    @Reference
    private Logger logger;

    private List<String> knowledgeDocs;

    public SystemSkill() {
        super(new String[]{"平台咨询", "en:Platform Help"},
                "当用户意图涉及理解和使用轻舟平台时激活此技能。具体激活场景包括但不限于：\n" +
                        "- 平台的设计理念、整体架构、核心特点与功能、适用范围；\n" +
                        "- 平台的目录结构、服务接口、前后端分离部署；\n" +
                        "- 平台的API和轻舟应用开发规范；");
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

        knowledgeDocs = docs;
    }

    @Override
    public String instruction() {
        return String.join("\n\n[参考附件]\n", knowledgeDocs);
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
