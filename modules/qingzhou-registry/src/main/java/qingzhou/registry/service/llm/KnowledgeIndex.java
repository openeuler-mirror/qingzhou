package qingzhou.registry.service.llm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KnowledgeIndex {

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int DEFAULT_MAX_RESULTS = 5;

    private final List<MarkdownSection> allSections = new ArrayList<>();
    private final MarkdownParser parser = new MarkdownParser();

    public void loadFromDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(dir.toPath(), 1)) {
            paths.filter(p -> p.toString().endsWith(".md"))
                    .forEach(p -> loadFromFile(p.toFile()));
        } catch (IOException e) {
            // ignore
        }
    }

    public void loadFromFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            String fileName = file.getName();
            List<MarkdownSection> sections = parser.parse(fileName, content);
            allSections.addAll(sections);
        } catch (IOException e) {
            // ignore
        }
    }

    public List<String> listTopics() {
        // 按文件分组，列出每个文件的顶级标题
        Map<String, List<String>> fileHeadings = new LinkedHashMap<>();
        for (MarkdownSection section : allSections) {
            fileHeadings.computeIfAbsent(section.getFileName(), k -> new ArrayList<>());
            if (section.getHeadingLevel() <= 2) {
                fileHeadings.get(section.getFileName()).add(section.getHeading());
            }
        }

        List<String> topics = new ArrayList<>();
        fileHeadings.forEach((file, headings) -> {
            topics.add("📄 " + file);
            headings.forEach(h -> topics.add("  - " + h));
        });
        return topics;
    }

    public List<Map<String, String>> search(String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return formatResults(allSections.stream()
                    .filter(s -> s.getHeadingLevel() == 0 || s.getHeadingLevel() == 1)
                    .limit(maxResults)
                    .collect(Collectors.toList()));
        }

        String lowerQuery = query.toLowerCase();
        // 对多个关键词（空格分隔）分别评分
        String[] keywords = lowerQuery.split("\\s+");

        Map<MarkdownSection, Integer> scores = new HashMap<>();
        for (MarkdownSection section : allSections) {
            int score = 0;
            String lowerHeading = section.getHeading().toLowerCase();
            String lowerContent = section.getContent().toLowerCase();

            for (String keyword : keywords) {
                if (lowerHeading.equals(keyword)) {
                    score += 10;
                } else if (lowerHeading.contains(keyword)) {
                    score += 5;
                }
                if (lowerContent.contains(keyword)) {
                    score += 3;
                }
            }

            // 顶级标题加分（更广泛的上下文）
            if (section.getHeadingLevel() <= 1) {
                score += 2;
            }

            if (score > 0) {
                scores.put(section, score);
            }
        }

        List<MarkdownSection> results = scores.entrySet().stream()
                .sorted(Map.Entry.<MarkdownSection, Integer>comparingByValue().reversed())
                .limit(maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return formatResults(results);
    }

    private List<Map<String, String>> formatResults(List<MarkdownSection> sections) {
        List<Map<String, String>> formatted = new ArrayList<>();
        for (MarkdownSection section : sections) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("source", section.getFileName());
            entry.put("heading", section.getHeading());
            if (section.getParentHeading() != null) {
                entry.put("parent", section.getParentHeading());
            }
            entry.put("content", truncate(section.getContent(), MAX_CONTENT_LENGTH));
            formatted.add(entry);
        }
        return formatted;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
