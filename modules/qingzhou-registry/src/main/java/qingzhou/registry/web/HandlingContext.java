package qingzhou.registry.web;

import java.util.Map;

public interface HandlingContext {
    String getParameter(String name);

    /**
     * 从工具入参构造上下文：值统一转字符串，缺失返回 null
     */
    static HandlingContext of(Map<String, Object> toolArgs) {
        return name -> {
            if (toolArgs == null) return null;
            Object val = toolArgs.get(name);
            return val != null ? String.valueOf(val) : null;
        };
    }
}
