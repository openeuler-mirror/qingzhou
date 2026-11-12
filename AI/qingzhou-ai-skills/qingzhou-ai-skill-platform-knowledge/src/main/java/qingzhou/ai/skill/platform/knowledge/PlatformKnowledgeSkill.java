package qingzhou.ai.skill.platform.knowledge;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.SkillService;
import qingzhou.common.Configs;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                SkillService.SKILL_NAME + "=平台知识",
                SkillService.SKILL_DESCRIPTION + "=当用户意图涉及轻舟平台的理解与使用时，激活本技能。适用场景包括但不限于：" +
                        "平台设计理念、整体架构、核心特性与功能、适用范围；平台目录结构、服务接口、前后端分离部署；平台 API 以及轻舟应用开发规范相关查询。"})
public class PlatformKnowledgeSkill implements SkillService {
    @Reference
    private Logger logger;

    // 知识库总量上限（字节）：文档随系统提示注入每轮对话，超限会放大内存与 token 消耗
    private long maxTotalDocBytes;
    private String instance;
    private String version;

    private volatile String instruction;

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        maxTotalDocBytes = configs.between("knowledge_doc_max_bytes", 1, 10 * 1024 * 1024, 1024 * 1024);
        instance = configs.get("qingzhou.instance");
        version = configs.get("qingzhou.version");
    }

    @Override
    public String instruction() {
        if (instruction == null) {
            synchronized (this) {
                if (instruction == null) {
                    initDocs();
                }
            }
        }

        return instruction;
    }

    private void initDocs() {
        StringBuilder msg = new StringBuilder();
        // version 直接拼入路径，须先拒绝穿越序列；instance 缺失或层级不足时用 Path 解析避免 NPE
        Path docsDir = Paths.get(instance).resolve("..").resolve("..")
                .resolve("lib").resolve("version" + version).resolve("docs").normalize();
        long appendedBytes = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(docsDir)) {
            for (Path file : stream) {
                // 跳过目录或其他非法文件
                if (!Files.isRegularFile(file)) continue;

                // 跳过突破总量上限的文件
                if (Files.size(file) > maxTotalDocBytes - appendedBytes) {
                    logger.warn("skip non-regular or too large file: " + file.getFileName() + " size: " + Files.size(file) / 1024 + "KB!");
                    continue;
                }

                // 跳过二进制文件。不限扩展名，按内容判定文本：含 NUL 或非法 UTF-8 替换符即视为二进制
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                if (content.isEmpty() || content.indexOf('\u0000') >= 0 || content.indexOf('\uFFFD') >= 0) {
                    logger.info("skip non-text file: " + file.getFileName());
                    continue;
                }

                msg.append("\n\n[参考附件] ").append(file.getFileName()).append(" \n").append(content);
                appendedBytes += Files.size(file);

                logger.info("read knowledge file: " + file.getFileName() + " size: " + Files.size(file) / 1024 + "KB");
            }
        } catch (Exception e) {
            logger.warn("failed to read knowledge", e);
        }

        instruction = msg.toString();
    }
}
