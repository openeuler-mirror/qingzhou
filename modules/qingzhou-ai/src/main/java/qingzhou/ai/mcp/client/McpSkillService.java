package qingzhou.ai.mcp.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;
import qingzhou.logger.Logger;

final class McpSkillService implements SkillService {
    private final McpConfig config;
    private final McpClient client;
    private final Logger logger;

    private final Object refreshLock = new Object();
    private volatile CachedTools cached;

    McpSkillService(McpConfig config, McpClient client, Logger logger) {
        this.config = config;
        this.client = client;
        this.logger = logger;
    }

    @Override
    public Map<ToolService, Map<String, Object>> tools() {
        if (isExpired()) refresh();

        CachedTools snapshot = cached;
        return snapshot == null ? Collections.emptyMap() : snapshot.tools;
    }

    private boolean isExpired() {
        CachedTools snapshot = cached;
        return snapshot == null;
    }

    private void refresh() {
        synchronized (refreshLock) {
            CachedTools snapshot = cached;
            if (snapshot != null) return;

            try {
                cached = build(client.listTools());
            } catch (Exception e) {
                // 保留旧缓存并进入退避期：故障 Server 不应让每轮对话都卡在连接超时上
                logger.warn("failed to load tools of mcp server [" + config.id + "]: " + e.getMessage());
                cached = new CachedTools(Collections.emptyMap());
            }
        }
    }

    private CachedTools build(List<Map<String, Object>> tools) {
        Map<ToolService, Map<String, Object>> declared = new HashMap<>();
        for (Map<String, Object> tool : tools) {
            String remote = text(tool.get("name"));
            if (remote.isEmpty()) continue;

            String exposed = McpToolConverter.exposedName(config.id, remote);
            Map<String, Object> properties = McpToolConverter.properties(exposed, text(tool.get("description")), tool.get("inputSchema"));
            declared.put(toolArgs -> client.callTool(remote, toolArgs), properties);
        }
        return new CachedTools(declared);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static final class CachedTools {
        final Map<ToolService, Map<String, Object>> tools;

        CachedTools(Map<ToolService, Map<String, Object>> tools) {
            this.tools = tools;
        }
    }
}
