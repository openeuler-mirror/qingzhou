package qingzhou.ai.skill;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                Paths.get(new File(instance).getParentFile().getParent(), "lib", "version" + version, "docs"),
                "*.md")) {
            for (Path md : stream) {
                List<String> lines = Files.readAllLines(md);
                if (!lines.isEmpty()) {
                    String content = String.join(System.lineSeparator(), lines);
                    msg.append("\n\n[参考附件]\n").append(content);
                }
            }
        } catch (Exception e) {
            logger.warn("failed to read knowledge", e);
        }
        instruction = msg.toString();
    }

    @Override
    public String instruction() {
        return instruction;
    }
}
