package qingzhou.llm.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;

import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.json.Json;
import qingzhou.llm.ChatModel;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;

public class Utils {
    public static void println(String msg) {
        System.err.println(msg); // 方便TW等集成，不用Logger对象
    }

    public static String errorMessage(Throwable t) {
        if (t == null) return "unknown error";
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    public static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(1000L << attempt); // 指数退避：1s / 2s / 4s
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 超过 maxChars 的文本截断并追加省略提示，避免长文本全量计入输入 token。
     */
    public static String truncate(String s, int maxChars) {
        if (s == null) return null;
        if (s.length() <= maxChars) return s;
        return s.substring(0, maxChars) + "\n…（内容过长已截断，原长度 " + s.length() + " 字符）";
    }

    // 按需加载技能恒
    public static List<Skill> getActiveSkills(Collection<Skill> skills, String message, ActiveSkillCallback callback) {
        if (skills == null || skills.isEmpty()) return Collections.emptyList();

        List<Skill> active = new ArrayList<>();
        List<Skill> candidates = new ArrayList<>();
        for (Skill skill : skills) {
            if (skill.required()) active.add(skill);
            else candidates.add(skill);
        }

        if (!candidates.isEmpty()) {
            Collection<Skill> matched1 = LiteralSkillMatcher.getInstance().match(candidates, message);
            if (!matched1.isEmpty()) {
                active.addAll(matched1);
                return active; // 词面强相关：直接激活，不再调用模型
            }

            // 选择模型须用无技能的独立 builder，避免共享技能配置导致匹配递归
            ChatModel selectionChatModel = callback.getUsedChatModel()
                    // 技能匹配只是“开场白”，收紧超时与重试：模型异常时应快速失败并提示，而不是把用户长时间晾在“思考中”
                    .connectTimeout(15_000)
                    .readTimeout(60_000)
                    .maxRetries(2)
                    .build();
            Collection<Skill> matched2 = new ModelSkillMatcher(selectionChatModel).match(candidates, message);
            if (!matched2.isEmpty()) {
                active.addAll(matched2);
                return active;
            }
        }
        return active;
    }

    // 激活技能的 tools 随对话挂载（显式工具 baseTools 恒挂载）
    public static Map<String, Tool> getActiveTools(Collection<Skill> activeSkills, Map<String, Tool> baseTools) {
        Map<String, Tool> tools = new HashMap<>(baseTools);
        for (Skill skill : activeSkills) {
            if (skill.tools() != null) {
                skill.tools().forEach(tool -> tools.put(tool.name(), tool));
            }
        }
        return tools;
    }

    public static String invokeTool(ToolCallInfo toolCallInfo, Map<String, Tool> tools, Json json) {
        Tool tool = tools.get(toolCallInfo.name);
        if (tool == null) return "Tool not found: " + toolCallInfo.name;

        Map<String, Object> args = null;
        if (toolCallInfo.arguments != null && !toolCallInfo.arguments.isEmpty()) {
            try {
                args = json.fromJson(toolCallInfo.arguments, Map.class);
            } catch (Exception ignored) {
            }
        }
        try {
            return tool.invoke(args);
        } catch (Throwable t) {
            // 仅回传异常概要，避免把堆栈/内部路径等敏感信息暴露给模型
            return "Error: " + Utils.errorMessage(t);
        }
    }

    public static Request newLlmRequest(Map<String, Object> llmRequest, boolean stream, ChatModelBuilderBase builder, HttpClient httpClient, Json json) throws Exception {
        Request request = httpClient.newRequest(builder.baseUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + builder.apiKey)
                .header("Accept", stream ? "text/event-stream" : "application/json");
        request.body(json.toJson(llmRequest).getBytes(StandardCharsets.UTF_8));
        request.connectTimeout(builder.connectTimeout);
        request.readTimeout(builder.readTimeout);
        return request;
    }

    public interface ActiveSkillCallback {
        ChatModelBuilderBase getUsedChatModel();
    }
}
