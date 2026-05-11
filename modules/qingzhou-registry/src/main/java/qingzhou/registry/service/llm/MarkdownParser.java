package qingzhou.registry.service.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownParser {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");

    public List<MarkdownSection> parse(String fileName, String markdownContent) {
        List<MarkdownSection> sections = new ArrayList<>();
        if (markdownContent == null || markdownContent.trim().isEmpty()) {
            return sections;
        }

        String[] lines = markdownContent.split("\n");
        // 标题层级栈，跟踪父标题
        String[] headingStack = new String[7]; // level 0-6
        StringBuilder currentContent = new StringBuilder();
        String currentHeading = fileName;
        int currentLevel = 0;
        String currentParent = null;

        for (String line : lines) {
            Matcher matcher = HEADING_PATTERN.matcher(line);
            if (matcher.matches()) {
                // 保存上一个 section
                String content = currentContent.toString().trim();
                if (!content.isEmpty() || !currentHeading.equals(fileName)) {
                    sections.add(new MarkdownSection(fileName, currentHeading, currentLevel, content, currentParent));
                }

                int level = matcher.group(1).length();
                String heading = matcher.group(2).trim();

                // 更新标题栈
                headingStack[level] = heading;
                // 清除更深层级的标题
                for (int i = level + 1; i < headingStack.length; i++) {
                    headingStack[i] = null;
                }

                // 父标题为上一级
                String parent = null;
                for (int i = level - 1; i >= 1; i--) {
                    if (headingStack[i] != null) {
                        parent = headingStack[i];
                        break;
                    }
                }

                currentHeading = heading;
                currentLevel = level;
                currentParent = parent;
                currentContent = new StringBuilder();
            } else {
                currentContent.append(line).append("\n");
            }
        }

        // 保存最后一个 section
        String content = currentContent.toString().trim();
        if (!content.isEmpty()) {
            sections.add(new MarkdownSection(fileName, currentHeading, currentLevel, content, currentParent));
        }

        return sections;
    }
}
