package qingzhou.ai.skill;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.SkillService;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                SkillService.SKILL_NAME + "=平台知识",
                SkillService.SKILL_DESCRIPTION + "=当用户意图涉及轻舟平台的理解与使用时，激活本技能。适用场景包括但不限于：" +
                        "平台设计理念、整体架构、核心特性与功能、适用范围；平台目录结构、服务接口、前后端分离部署；平台 API 以及轻舟应用开发规范相关查询。"})
public class PlatformKnowledgeSkill implements SkillService {
    // 知识库总量上限：文档随系统提示注入每轮对话，超限会放大内存与 token 消耗
    private static final int MAX_TOTAL_DOC_CHARS = 256 * 1024;

    @Reference
    private Logger logger;

    private String instruction;

    @Activate
    public void init(Map<String, String> config) {
        StringBuilder msg = new StringBuilder(
                "你精通轻舟平台整体架构、核心特性与设计理念，熟悉 Java 生态、低代码开发、声明式开发、RESTful API 设计及动态渲染技术。" +
                        "当用户询问 AI 管控使用方式时，说明可通过自然语言对系统进行管理。");
        String instance = config.get("qingzhou.instance");
        String version = config.get("qingzhou.version");
        // version 直接拼入路径，须先拒绝穿越序列；instance 缺失或层级不足时用 Path 解析避免 NPE
        if (!version.contains("..") && !version.contains("/") && !version.contains("\\")) {
            Path docsDir = Paths.get(instance).resolve("..").resolve("..")
                    .resolve("lib").resolve("version" + version).resolve("docs").normalize();
            int appendedChars = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(docsDir, "*.md")) {
                for (Path md : stream) {
                    if (Files.size(md) > MAX_TOTAL_DOC_CHARS - appendedChars) continue;
                    String content = new String(Files.readAllBytes(md), StandardCharsets.UTF_8);
                    if (content.isEmpty()) continue;
                    msg.append("\n\n[参考附件]\n").append(content);
                    appendedChars += content.length();
                }
            } catch (Exception e) {
                logger.warn("failed to read knowledge", e);
            }
        }
        instruction = msg.toString();
    }

    @Override
    public String instruction() {
        return instruction;
    }
}
