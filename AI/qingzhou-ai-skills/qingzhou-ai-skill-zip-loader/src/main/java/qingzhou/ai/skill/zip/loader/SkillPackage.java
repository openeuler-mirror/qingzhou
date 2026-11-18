package qingzhou.ai.skill.zip.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;

/**
 * 技能包（zip）对应的静态技能：frontmatter 提供名称与描述，正文作为说明书；
 * 系统提示仅列出包根与一级子目录的文本文件清单，避免挤爆上下文；
 * 包内所有文本文件均可由 read-file 工具按路径按需读取（含深层目录如 API 源码）。
 */
final class SkillPackage implements SkillService {
    private static final String MANIFEST = "SKILL.md";
    private static final String READ_TOOL_SUFFIX = "-read-file";
    private static final int READ_PAGE_CHARS = 8000; // 单次返回上限：文本页过大时模型无法消化，剩余部分由 offset 续读

    private final String name;
    private final String description;
    private final String instruction;
    private final Map<String, String> files;
    private final Map<ToolService, Map<String, Object>> tools;

    private SkillPackage(String name, String description, String body, Map<String, String> files) {
        this.name = name;
        this.description = description;
        this.files = files;
        this.instruction = buildInstruction(body);
        this.tools = buildTools();
    }

    @Override
    public String instruction() {
        return instruction;
    }

    @Override
    public Map<ToolService, Map<String, Object>> tools() {
        return tools;
    }

    String name() {
        return name;
    }

    String description() {
        return description;
    }

    static SkillPackage load(Path zipFile, int maxBytes) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            ZipEntry manifestEntry = findManifest(zip);
            String root = parent(manifestEntry.getName());
            byte[] manifestBytes = read(zip, manifestEntry, maxBytes);
            if (manifestBytes == null) {
                throw new IllegalArgumentException(MANIFEST + " exceeds " + maxBytes + " bytes");
            }

            String[] head = splitFrontmatter(new String(manifestBytes, StandardCharsets.UTF_8));
            Map<String, String> metadata = parseMetadata(head[0]);
            String name = metadata.get("name");
            String description = metadata.get("description");
            if (name == null || description == null) {
                throw new IllegalArgumentException("missing name or description in " + MANIFEST);
            }
            // 名称会拼进工具名，OpenAI 函数名只允许字母、数字、下划线与连字符
            if (!name.matches("[A-Za-z0-9_-]{1,50}")) {
                throw new IllegalArgumentException("name must match [A-Za-z0-9_-]{1,50}: " + name);
            }

            Map<String, String> files = new LinkedHashMap<>();
            for (ZipEntry entry : attachments(zip, root)) {
                String text = new String(read(zip, entry, Integer.MAX_VALUE), StandardCharsets.UTF_8);
                if (text.isEmpty() || text.indexOf('\u0000') >= 0 || text.indexOf('\uFFFD') >= 0) continue;
                files.put(entry.getName().substring(root.length()), text);
            }
            return new SkillPackage(name, description, head[1].trim(), files);
        }
    }

    private String buildInstruction(String body) {
        StringBuilder instruction = new StringBuilder(body);
        instruction.append("\n\n需要参考其他文档时，调用工具 ").append(readToolName())
                .append(" 按路径读取（单次最多 ").append(READ_PAGE_CHARS).append(" 字符，未读完用 offset 续读）。")
                .append("包根与一级子目录文件如下，深层文件（如 API 源码）未列出但可按需读取：");
        files.forEach((path, text) -> {
            if (path.chars().filter(c -> c == '/').count() <= 1) {
                instruction.append("\n- ").append(path).append("（").append(text.length()).append(" 字符）");
            }
        });
        return instruction.toString();
    }

    private Map<ToolService, Map<String, Object>> buildTools() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(ToolService.TOOL_NAME, readToolName());
        properties.put(ToolService.TOOL_DESCRIPTION, "读取本技能包内的参考文档；path 为相对路径，含未在文件列表中列出的深层文件");
        properties.put(ToolService.PARAMETER_NAME + ".1", "path");
        properties.put(ToolService.PARAMETER_DESCRIPTION + ".1", "参考文档的相对路径");
        properties.put(ToolService.PARAMETER_REQUIRED + ".1", "true");
        properties.put(ToolService.PARAMETER_TYPE + ".1", "string");
        properties.put(ToolService.PARAMETER_NAME + ".2", "offset");
        properties.put(ToolService.PARAMETER_DESCRIPTION + ".2", "起始字符位置，默认 0；返回内容末尾会给出下次续读的 offset");
        properties.put(ToolService.PARAMETER_REQUIRED + ".2", "false");
        properties.put(ToolService.PARAMETER_TYPE + ".2", "integer");

        Map<ToolService, Map<String, Object>> declared = new HashMap<>();
        declared.put(this::readFile, properties);
        return declared;
    }

    private String readToolName() {
        return name + READ_TOOL_SUFFIX;
    }

    private String readFile(Map<String, Object> args) {
        String path = args.get("path") == null ? null : args.get("path").toString();
        String content = files.get(path);
        if (content == null) {
            throw new IllegalArgumentException("unknown file: " + path);
        }

        int offset = args.get("offset") == null ? 0 : Integer.parseInt(args.get("offset").toString().trim());
        if (offset < 0 || offset >= content.length()) {
            throw new IllegalArgumentException("offset must be in [0, " + content.length() + "): " + offset);
        }

        int end = Math.min(content.length(), offset + READ_PAGE_CHARS);
        String page = content.substring(offset, end);
        return end < content.length()
                ? page + "\n\n[已返回字符 " + offset + "~" + end + "，共 " + content.length() + "，续读请传 offset=" + end + "]"
                : page;
    }

    private static ZipEntry findManifest(ZipFile zip) {
        ZipEntry found = null;
        for (ZipEntry entry : Collections.list(zip.entries())) {
            String name = entry.getName();
            if (!name.equals(MANIFEST) && !name.endsWith("/" + MANIFEST)) continue;

            if (found != null) throw new IllegalArgumentException("multiple " + MANIFEST + " found");
            found = entry;
        }
        if (found == null) throw new IllegalArgumentException(MANIFEST + " not found");
        return found;
    }

    // 收集包内所有文本文件（供 read-file 工具按需读取）；排除目录项与隐藏文件
    private static List<ZipEntry> attachments(ZipFile zip, String root) {
        List<ZipEntry> entries = new ArrayList<>();
        for (ZipEntry entry : Collections.list(zip.entries())) {
            String name = entry.getName();
            if (!name.startsWith(root) || name.equals(root + MANIFEST)) continue;

            String relative = name.substring(root.length());
            if (relative.isEmpty() || relative.endsWith("/")) continue;
            if (relative.substring(relative.lastIndexOf('/') + 1).startsWith(".")) continue;
            entries.add(entry);
        }
        entries.sort((a, b) -> a.getName().compareTo(b.getName()));
        return entries;
    }

    // 返回 [frontmatter, 正文]；frontmatter 缺失即视为非法包
    private static String[] splitFrontmatter(String text) {
        String[] lines = text.split("\\R", -1);
        if (lines.length == 0 || !"---".equals(lines[0].trim())) {
            throw new IllegalArgumentException("missing frontmatter in " + MANIFEST);
        }
        int end = 1;
        while (end < lines.length && !"---".equals(lines[end].trim())) end++;
        if (end >= lines.length) throw new IllegalArgumentException("unclosed frontmatter in " + MANIFEST);

        String header = String.join("\n", Arrays.copyOfRange(lines, 1, end));
        String body = String.join("\n", Arrays.copyOfRange(lines, end + 1, lines.length));
        return new String[]{header, body};
    }

    private static Map<String, String> parseMetadata(String header) {
        Map<String, String> metadata = new HashMap<>();
        for (String line : header.split("\\R")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String value = line.substring(colon + 1).trim();
            if (value.length() > 1 && (value.startsWith("\"") && value.endsWith("\"")
                    || value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            metadata.put(line.substring(0, colon).trim(), value);
        }
        return metadata;
    }

    private static String parent(String entryName) {
        int slash = entryName.lastIndexOf('/');
        return slash < 0 ? "" : entryName.substring(0, slash + 1);
    }

    // 读到 limit 字节仍未结束时返回 null，防止压缩包内声明的大小与实际内容不符
    private static byte[] read(ZipFile zip, ZipEntry entry, int limit) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > limit) return null;
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        }
    }
}
