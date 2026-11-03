package qingzhou.ai.skill;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import qingzhou.ai.ToolService;
import qingzhou.api.QingzhouApp;
import qingzhou.json.Json;

public class AppBuilderSkillTest {
    private final AppBuilderSkill skill = new AppBuilderSkill();
    private Path instance;
    private String apiMaterials;

    @BeforeClass
    public void init() throws Exception {
        Path product = Files.createTempDirectory(Paths.get("target"), "app-builder-中文 空格-").toRealPath();
        instance = Files.createDirectories(product.resolve("instances/default"));
        Path library = Files.createDirectories(product.resolve("lib/versiontest-1.0"));
        dependency(QingzhouApp.class, library.resolve("modules/qingzhou-api.jar"));
        dependency(Json.class, library.resolve("components/qingzhou-json.jar"));
        Path repository = Paths.get("../..").toRealPath();
        Path api = repository.resolve("modules/qingzhou-api/src/main/java");
        StringBuilder expectedApi = new StringBuilder();
        try (Stream<Path> stream = Files.walk(api)) {
            for (Path source : stream.filter(path -> path.toString().endsWith(".java")).sorted().collect(Collectors.toList())) {
                Path target = library.resolve("docs/qingzhou-api").resolve(api.relativize(source));
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
                expectedApi.append("\n[文件：").append(api.relativize(source)).append("]\n").append(readUtf8(source));
            }
        }
        apiMaterials = expectedApi.toString();
        Path prompt = library.resolve("docs/rest-app-generation/README.md");
        Files.createDirectories(prompt.getParent());
        Files.copy(repository.resolve("docs/rest-app-generation/README.md"), prompt);
        Map<String, String> config = new HashMap<>();
        config.put("qingzhou.instance", instance.toString());
        config.put("qingzhou.version", "test-1.0");
        skill.init(config);
    }

    @Test
    public void materialsAreCompleteAndBelowToolLimit() throws Exception {
        Assert.assertEquals(skill.tools().size(), 4);
        Assert.assertTrue(skill.instruction().length() < 10000);
        StringBuilder api = new StringBuilder();
        String offset = "0";
        do {
            String page = invoke("app_builder_materials", "offset", offset);
            Assert.assertTrue(page.length() < 2000);
            api.append(page.substring(page.indexOf('\n') + 1));
            offset = page.substring(page.indexOf("nextOffset=") + 11, page.indexOf('\n'));
        } while (!offset.equals("END"));
        Assert.assertEquals(api.toString(), apiMaterials);
    }

    @Test
    public void toolParametersDeclareStringTypes() {
        for (Map<String, Object> properties : skill.tools().values()) {
            for (String key : properties.keySet()) {
                if (key.startsWith(ToolService.PARAMETER_NAME + ".")) {
                    String suffix = key.substring(ToolService.PARAMETER_NAME.length());
                    Assert.assertEquals(properties.get(ToolService.PARAMETER_TYPE + suffix), "string");
                }
            }
        }
    }

    @Test
    public void reopeningPreservesManualChanges() throws Exception {
        invoke("app_builder_project", "app", "manual");
        Path pom = project("manual").resolve("pom.xml");
        String edited = readUtf8(pom) + "\n<!-- 人工修改 -->";
        Files.write(pom, edited.getBytes(StandardCharsets.UTF_8));
        invoke("app_builder_source", "app", "manual", "path", "src/main/java/demo/Manual.java", "content", "package demo; class Manual {} ");
        invoke("app_builder_project", "app", "manual");
        Assert.assertEquals(readUtf8(pom), edited);
        Assert.assertTrue(invoke("app_builder_source", "app", "manual").contains("src/main/java/demo/Manual.java"));
        Assert.assertTrue(invoke("app_builder_source", "app", "manual", "path", "src/main/java/demo/Manual.java").contains("class Manual"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsInvalidApp() throws Exception {
        invoke("app_builder_project", "app", "../escape");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsSourceTraversal() throws Exception {
        invoke("app_builder_project", "app", "paths");
        invoke("app_builder_source", "app", "paths", "path", "src/main/java/../../outside.java", "content", "class Outside {}");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsInvalidReadOffset() throws Exception {
        invoke("app_builder_materials", "offset", "-1");
    }

    @Test
    public void emptyProjectIsNotDeployed() throws Exception {
        if (!"1.8".equals(System.getProperty("java.specification.version"))) throw new SkipException("需要 JDK 1.8");
        invoke("app_builder_project", "app", "empty");
        Assert.assertTrue(invoke("app_builder_build", "app", "empty").contains("没有 Java 源码"));
        Assert.assertFalse(Files.exists(instance.resolve("apps/qingzhou-app-empty.jar")));
    }

    @Test
    public void buildFailureRepairAndConflict() throws Exception {
        if (!"1.8".equals(System.getProperty("java.specification.version"))) throw new SkipException("需要 JDK 1.8 和 PATH 中的 Maven");
        invoke("app_builder_project", "app", "build-check");
        String path = "src/main/java/demo/Generated.java";
        invoke("app_builder_source", "app", "build-check", "path", path, "content", "package demo; public class Generated { notJava }");
        String failure = invoke("app_builder_build", "app", "build-check");
        Assert.assertTrue(failure.contains("构建失败"), failure);
        Assert.assertTrue(failure.contains("Generated.java"), failure);
        Path deployed = instance.resolve("apps/qingzhou-app-build-check.jar");
        Assert.assertFalse(Files.exists(deployed));
        String source = "package demo; @qingzhou.api.App(code=\"build-check\",name={\"构建验证\"}) " +
                "public class Generated implements qingzhou.api.QingzhouApp { " +
                "public void start(qingzhou.api.AppContext context) { throw new AssertionError(\"构建不得运行应用\"); } " +
                "public qingzhou.json.Json json; }";
        Files.write(project("build-check").resolve(path), source.getBytes(StandardCharsets.UTF_8));
        invoke("app_builder_project", "app", "build-check");
        String success = invoke("app_builder_build", "app", "build-check");
        Assert.assertTrue(success.contains("构建成功"), success);
        try (JarFile jar = new JarFile(deployed.toFile());
             DataInputStream bytecode = new DataInputStream(jar.getInputStream(jar.getJarEntry("demo/Generated.class")))) {
            Assert.assertEquals(bytecode.readInt(), 0xCAFEBABE);
            bytecode.readUnsignedShort();
            Assert.assertEquals(bytecode.readUnsignedShort(), 52);
            Assert.assertNull(jar.getJarEntry("qingzhou/api/App.class"));
        }
        byte[] original = Files.readAllBytes(deployed);
        String conflict = invoke("app_builder_build", "app", "build-check");
        Assert.assertTrue(conflict.contains("apps 已有"), conflict);
        Assert.assertEquals(Files.readAllBytes(deployed), original);
        Assert.assertEquals(readUtf8(project("build-check").resolve(path)), source);
    }

    private static String readUtf8(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private Path project(String app) {
        return instance.resolve("temp/app-builder/qingzhou-app-" + app);
    }

    private String invoke(String name, String... pairs) throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) arguments.put(pairs[i], pairs[i + 1]);
        for (Map.Entry<ToolService, Map<String, Object>> tool : skill.tools().entrySet()) {
            if (name.equals(tool.getValue().get(ToolService.TOOL_NAME))) return tool.getKey().invoke(arguments);
        }
        throw new AssertionError(name);
    }

    private static void dependency(Class<?> type, Path target) throws IOException, URISyntaxException {
        Files.createDirectories(target.getParent());
        Path classes = Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!Files.isDirectory(classes)) {
            Files.copy(classes, target);
            return;
        }
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(target)); Stream<Path> stream = Files.walk(classes)) {
            for (Path file : stream.filter(Files::isRegularFile).collect(Collectors.toList())) {
                jar.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, jar);
                jar.closeEntry();
            }
        }
    }
}
