package qingzhou.ai.skill.registry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
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
    private static final String PARAMETER_PATH = "path";
    private static final String PARAMETER_OFFSET = "offset";
    private static final int READ_PAGE_CHARS = 8000; // 单次返回上限：文本页过大时模型无法消化，剩余部分由 offset 续读

    private final String name;
    private final String description;
    private final String instruction;
    private final Map<String, String> files;
    private final Map<ToolService, Map<String, Object>> tools;

    private SkillPackage(Manifest head, Map<String, String> files) {
        this.name = head.name;
        this.description = head.description;
        this.files = files;
        this.instruction = buildInstruction(head.body);
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
            Map<String, ZipEntry> entries = new TreeMap<>(); // 有序遍历，保证提示词里的文件清单稳定
            ZipEntry manifest = null;
            for (ZipEntry entry : Collections.list(zip.entries())) {
                String entryName = entry.getName();
                if (entryName.equals(MANIFEST) || entryName.endsWith("/" + MANIFEST)) {
                    if (manifest != null) throw new IllegalArgumentException("multiple " + MANIFEST + " found");
                    manifest = entry;
                }
                entries.put(entryName, entry);
            }
            if (manifest == null) throw new IllegalArgumentException(MANIFEST + " not found");

            byte[] manifestBytes = read(zip, manifest, maxBytes);
            if (manifestBytes == null) {
                throw new IllegalArgumentException(MANIFEST + " exceeds " + maxBytes + " bytes");
            }
            Manifest head = new Manifest(new String(manifestBytes, StandardCharsets.UTF_8));
            // 名称会拼进工具名，OpenAI 函数名只允许字母、数字、下划线与连字符
            if (!head.name.matches("[A-Za-z0-9_-]{1,50}")) {
                throw new IllegalArgumentException("name must match [A-Za-z0-9_-]{1,50}: " + head.name);
            }

            String manifestName = manifest.getName();
            String root = manifestName.substring(0, manifestName.lastIndexOf('/') + 1);
            Map<String, String> files = new LinkedHashMap<>();
            for (Map.Entry<String, ZipEntry> entry : entries.entrySet()) {
                String entryName = entry.getKey();
                if (!entryName.startsWith(root) || entryName.equals(manifestName)) continue;

                String relative = entryName.substring(root.length());
                if (relative.isEmpty() || relative.endsWith("/")) continue;
                if (relative.substring(relative.lastIndexOf('/') + 1).startsWith(".")) continue; // 隐藏文件不入清单

                byte[] content = read(zip, entry.getValue(), maxBytes);
                if (content == null) {
                    throw new IllegalArgumentException(relative + " exceeds " + maxBytes + " bytes");
                }
                String text = new String(content, StandardCharsets.UTF_8);
                if (text.isEmpty() || text.indexOf('\u0000') >= 0 || text.indexOf('\uFFFD') >= 0) continue; // 跳过空文件与二进制
                files.put(relative, text);
            }
            return new SkillPackage(head, files);
        }
    }

    private String buildInstruction(String body) {
        StringBuilder instruction = new StringBuilder(body);
        instruction.append("\n\n需要参考其他文档时，调用工具 ").append(name).append(READ_TOOL_SUFFIX)
                .append(" 按路径读取（单次最多 ").append(READ_PAGE_CHARS).append(" 字符，未读完用 ").append(PARAMETER_OFFSET).append(" 续读）。")
                .append("包根与一级子目录文件如下，深层文件（如 API 源码）未列出但可按需读取：");
        files.forEach((path, text) -> {
            if (path.indexOf('/', path.indexOf('/') + 1) < 0) {
                instruction.append("\n- ").append(path).append("（").append(text.length()).append(" 字符）");
            }
        });
        return instruction.toString();
    }

    private Map<ToolService, Map<String, Object>> buildTools() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(ToolService.TOOL_NAME, name + READ_TOOL_SUFFIX);
        properties.put(ToolService.TOOL_DESCRIPTION, "读取本技能包内的参考文档；" + PARAMETER_PATH + " 为相对路径，含未在文件列表中列出的深层文件");
        properties.put(ToolService.PARAMETER_NAME + ".1", PARAMETER_PATH);
        properties.put(ToolService.PARAMETER_DESCRIPTION + ".1", "参考文档的相对路径");
        properties.put(ToolService.PARAMETER_REQUIRED + ".1", "true");
        properties.put(ToolService.PARAMETER_TYPE + ".1", "string");
        properties.put(ToolService.PARAMETER_NAME + ".2", PARAMETER_OFFSET);
        properties.put(ToolService.PARAMETER_DESCRIPTION + ".2", "起始字符位置，默认 0；返回内容末尾会给出下次续读的 " + PARAMETER_OFFSET);
        properties.put(ToolService.PARAMETER_REQUIRED + ".2", "false");
        properties.put(ToolService.PARAMETER_TYPE + ".2", "integer");
        return Collections.singletonMap(this::readFile, properties);
    }

    private String readFile(Map<String, Object> args) {
        String content = files.get(args.get(PARAMETER_PATH));
        if (content == null) {
            throw new IllegalArgumentException("unknown file: " + args.get(PARAMETER_PATH));
        }

        int offset = args.get(PARAMETER_OFFSET) == null ? 0 : Integer.parseInt(args.get(PARAMETER_OFFSET).toString().trim());
        if (offset < 0 || offset >= content.length()) {
            throw new IllegalArgumentException("offset must be in [0, " + content.length() + "): " + offset);
        }

        int end = Math.min(content.length(), offset + READ_PAGE_CHARS);
        String page = content.substring(offset, end);
        return end < content.length()
                ? page + "\n\n[已返回字符 " + offset + "~" + end + "，共 " + content.length() + "，续读请传 " + PARAMETER_OFFSET + "=" + end + "]"
                : page;
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

    // frontmatter 仅取 name/description，其余键忽略；正文为第二个 --- 之后的全部内容，缺失即视为非法包
    private static final class Manifest {
        private final String name;
        private final String description;
        private final String body;

        private Manifest(String text) {
            String[] lines = text.split("\\R", -1);
            if (!"---".equals(lines[0].trim())) {
                throw new IllegalArgumentException("missing frontmatter in " + MANIFEST);
            }
            int end = 1;
            while (end < lines.length && !"---".equals(lines[end].trim())) end++;
            if (end >= lines.length) throw new IllegalArgumentException("unclosed frontmatter in " + MANIFEST);

            String name = null;
            String description = null;
            for (int i = 1; i < end; i++) {
                int colon = lines[i].indexOf(':');
                if (colon <= 0) continue;
                String key = lines[i].substring(0, colon).trim();
                if (!key.equals("name") && !key.equals("description")) continue;

                String value = lines[i].substring(colon + 1).trim();
                if (value.length() > 1 && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (key.equals("name")) name = value;
                else description = value;
            }
            if (name == null || description == null) {
                throw new IllegalArgumentException("missing name or description in " + MANIFEST);
            }
            this.name = name;
            this.description = description;
            this.body = String.join("\n", Arrays.copyOfRange(lines, end + 1, lines.length)).trim();
        }
    }
}
