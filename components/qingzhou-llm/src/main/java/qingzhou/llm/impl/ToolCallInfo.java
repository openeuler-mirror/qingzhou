package qingzhou.llm.impl;

// 内部 DTO：流式路径中字段被增量填充，经访问器读写以防外部直接篡改内部状态
public class ToolCallInfo {
    private String id;
    private String name;
    private String arguments;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }
}
