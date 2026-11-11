package qingzhou.ai.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.ai.ToolService;

// 插件工具注册表：随 OSGi 服务增删维护 tools/list 快照
class ToolRegistry {
    private final Map<String, ToolCache> tools = new ConcurrentHashMap<>();
    private volatile List<Map<String, Object>> toolSchemas = Collections.emptyList();

    // 增删与快照重建同锁：两者交错会让 toolSchemas 与 tools 不一致
    synchronized void put(ToolCache toolCache) {
        ToolCache previous = tools.putIfAbsent(toolCache.toolName, toolCache);
        if (previous != null) {
            if (previous.toolService != toolCache.toolService) {
                throw new IllegalArgumentException("duplicate MCP tool name: " + toolCache.toolName);
            }
            tools.put(toolCache.toolName, toolCache); // 同一服务重新绑定：刷新缓存
        }
        rebuild();
    }

    synchronized void remove(ToolService tool) {
        tools.values().removeIf(toolCache -> toolCache.toolService == tool);
        rebuild();
    }

    ToolCache find(String toolName) {
        return tools.get(toolName);
    }

    List<Map<String, Object>> schemas() {
        return toolSchemas;
    }

    private void rebuild() {
        List<Map<String, Object>> schemas = new ArrayList<>(tools.size());
        for (ToolCache toolCache : tools.values()) {
            schemas.add(toolCache.toolSchema);
        }
        toolSchemas = Collections.unmodifiableList(schemas);
    }
}
