package qingzhou.ai.mcp.client;

import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;
import qingzhou.logger.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class McpSkillService implements SkillService {
    private static final long RETRY_BACKOFF_MILLIS = 60_000;

    private final McpConfig config;
    private final McpClient client;
    private final Logger logger;
    private final long cacheMillis;

    private final Object refreshLock = new Object();
    private volatile CachedTools cached;

    McpSkillService(McpConfig config, McpClient client, Logger logger, long cacheMillis) {
        this.config = config;
        this.client = client;
        this.logger = logger;
        this.cacheMillis = cacheMillis;
    }

    @Override
    public Map<ToolService, Map<String, Object>> tools() {
        if (isExpired()) refresh();

        CachedTools snapshot = cached;
        return snapshot == null ? Collections.emptyMap() : snapshot.tools;
    }

    private boolean isExpired() {
        CachedTools snapshot = cached;
        return snapshot == null || System.currentTimeMillis() >= snapshot.nextRefreshAt;
    }

    private void refresh() {
        synchronized (refreshLock) {
            CachedTools snapshot = cached;
            if (snapshot != null && System.currentTimeMillis() < snapshot.nextRefreshAt) return;

            try {
                cached = build(client.listTools(), System.currentTimeMillis() + cacheMillis);
            } catch (Exception e) {
                // 保留旧缓存并进入退避期：故障 Server 不应让每轮对话都卡在连接超时上
                logger.warn("failed to load tools of mcp server [" + config.id + "]: " + e.getMessage());
                long retryAt = System.currentTimeMillis() + RETRY_BACKOFF_MILLIS;
                cached = snapshot == null ? new CachedTools(Collections.emptyMap(), retryAt)
                        : new CachedTools(snapshot.tools, retryAt);
            }
        }
    }

    private CachedTools build(List<Map<String, Object>> tools, long nextRefreshAt) {
        Map<ToolService, Map<String, Object>> declared = new HashMap<>();
        for (Map<String, Object> tool : tools) {
            String remote = text(tool.get("name"));
            if (remote.isEmpty()) continue;

            String exposed = McpToolConverter.exposedName(config.id, remote);
            Map<String, Object> properties = McpToolConverter.properties(exposed, text(tool.get("description")), tool.get("inputSchema"));
            declared.put(toolArgs -> client.callTool(remote, toolArgs), properties);
        }
        return new CachedTools(declared, nextRefreshAt);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static final class CachedTools {
        final Map<ToolService, Map<String, Object>> tools;
        final long nextRefreshAt;

        CachedTools(Map<ToolService, Map<String, Object>> tools, long nextRefreshAt) {
            this.tools = tools;
            this.nextRefreshAt = nextRefreshAt;
        }
    }
}
