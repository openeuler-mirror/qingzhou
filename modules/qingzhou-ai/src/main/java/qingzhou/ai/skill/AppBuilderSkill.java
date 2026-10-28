package qingzhou.ai.skill;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;

@Component(configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                SkillService.SKILL_NAME + "=应用构建",
                SkillService.SKILL_DESCRIPTION + "=根据 REST 接口文档创建轻舟应用；生成、查看或修改应用源码，" +
                        "创建 Maven 工程、编译打包、继续修复应用构建，并放置应用 JAR。"})
public class AppBuilderSkill implements SkillService {
    private final Map<ToolService, Map<String, Object>> tools = new LinkedHashMap<>();
    private String instruction;

    @Activate
    public void init(Map<String, String> config) throws IOException {
        Path instance = Paths.get(config.get("qingzhou.instance")).toRealPath();
        String version = config.get("qingzhou.version");
        if (!version.matches("[A-Za-z0-9_.-]+") || version.contains("..")) {
            throw new IOException("无效的轻舟版本号");
        }
        Path library = instance.resolve("../../lib/version" + version).normalize();
        Path docs = library.resolve("docs");
        instruction = resource("AppBuilderSkill.md") + "\n\n[第一阶段生成规则]\n" +
                read(docs.resolve("rest-app-generation/README.md"));
        StringBuilder api = new StringBuilder();
        Path apiRoot = docs.resolve("qingzhou-api");
        try (Stream<Path> stream = Files.walk(apiRoot)) {
            List<Path> sources = stream.filter(path -> path.toString().endsWith(".java"))
                    .sorted().collect(Collectors.toList());
            if (sources.isEmpty()) throw new IOException("缺少完整 qingzhou-api 源码，请重新构建产品");
            for (Path source : sources) {
                api.append("\n[文件：").append(apiRoot.relativize(source)).append("]\n").append(read(source));
            }
        }
        String materials = api.toString();
        AppBuilderProjects projects = new AppBuilderProjects(instance, library, version, resource("app-pom.xml"));
        tools.clear();
        tool(args -> page(materials, value(args, "offset")), "app_builder_materials",
                "分段读取与当前轻舟产品同版本的完整 API 源码，按 nextOffset 续读到末尾。",
                "offset", "字符偏移，首次为 0", "false");
        tool(args -> projects.create(value(args, "app")), "app_builder_project",
                "创建或打开独立 Maven 工程，保留已有源码和 POM；返回人工修改用的工程目录。",
                "app", "应用标识，小写字母开头，可含小写字母、数字和连字符，最长 64 字符", "true");
        tool(args -> projects.source(value(args, "app"), value(args, "path"),
                        value(args, "content"), value(args, "offset")), "app_builder_source",
                "读写应用 Java 源码。省略 path 列出源码；省略 content 分段读取文件；提供 content 保存完整文件。",
                "app", "已创建的应用标识", "true",
                "path", "src/main/java/ 开头的相对 Java 文件路径", "false",
                "content", "完整 Java 源码；读取或列目录时不要提供此参数", "false",
                "offset", "读取时的字符偏移，首次为 0", "false");
        tool(args -> projects.build(value(args, "app")), "app_builder_build",
                "在工程目录用 JDK 1.8 执行 mvn -B clean package；成功后将 JAR 放入当前实例 apps，不覆盖已有包。",
                "app", "已创建的应用标识", "true");
    }

    private void tool(ToolService tool, String name, String description, String... parameters) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(ToolService.TOOL_NAME, name);
        properties.put(ToolService.TOOL_DESCRIPTION, description);
        for (int i = 0; i < parameters.length; i += 3) {
            String suffix = "." + i / 3;
            properties.put(ToolService.PARAMETER_NAME + suffix, parameters[i]);
            properties.put(ToolService.PARAMETER_DESCRIPTION + suffix, parameters[i + 1]);
            properties.put(ToolService.PARAMETER_REQUIRED + suffix, parameters[i + 2]);
            properties.put(ToolService.PARAMETER_TYPE + suffix, "string");
        }
        tools.put(tool, properties);
    }

    private static String value(Map<String, Object> args, String key) {
        return args == null ? null : (String) args.get(key);
    }

    static String page(String text, String offset) {
        int start = offset == null ? 0 : Integer.parseInt(offset);
        if (start < 0 || start > text.length()) throw new IllegalArgumentException("读取偏移超出范围");
        int end = start + Math.min(1700, text.length() - start);
        return "offset=" + start + ", total=" + text.length() + ", nextOffset=" +
                (end < text.length() ? end : "END") + "\n" + text.substring(start, end);
    }

    static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static String resource(String name) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                AppBuilderSkill.class.getResourceAsStream(name), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @Override
    public String instruction() {
        return instruction;
    }

    @Override
    public Map<ToolService, Map<String, Object>> tools() {
        return tools;
    }
}
