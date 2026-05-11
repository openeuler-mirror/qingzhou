package qingzhou.registry.service.llm;

public class MarkdownSection {
    private final String fileName;
    private final String heading;
    private final int headingLevel;
    private final String content;
    private final String parentHeading;

    public MarkdownSection(String fileName, String heading, int headingLevel, String content, String parentHeading) {
        this.fileName = fileName;
        this.heading = heading;
        this.headingLevel = headingLevel;
        this.content = content;
        this.parentHeading = parentHeading;
    }

    public String getFileName() {
        return fileName;
    }

    public String getHeading() {
        return heading;
    }

    public int getHeadingLevel() {
        return headingLevel;
    }

    public String getContent() {
        return content;
    }

    public String getParentHeading() {
        return parentHeading;
    }
}
