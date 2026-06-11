package qingzhou.llm;

import java.util.Map;

public interface ChatContext {
    // 可以存放用户身份、权限等上下文信息
    Map<String, Object> attributes();

    // 获取上下文中最新的用户意图
    String getLatestMessage();
}
