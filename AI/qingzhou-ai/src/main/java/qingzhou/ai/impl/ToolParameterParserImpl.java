package qingzhou.ai.impl;

import java.util.*;
import java.util.function.BiConsumer;

import org.osgi.service.component.annotations.Component;
import qingzhou.ai.ToolParameterParser;
import qingzhou.ai.ToolService;

@Component
public class ToolParameterParserImpl implements ToolParameterParser {
    private static final Set<String> SCHEMA_TYPES = new HashSet<>(Arrays.asList(
            "string", "number", "integer", "boolean", "array", "object", "null"));
    private static final Map<String, BiConsumer<ToolParameter, String>> BINDERS = new HashMap<>();

    static {
        BINDERS.put(ToolService.PARAMETER_NAME, (p, v) -> {
            if (v == null || v.isEmpty()) {
                throw new IllegalArgumentException("missing " + ToolService.PARAMETER_NAME);
            }
            p.name = v;
        });
        BINDERS.put(ToolService.PARAMETER_DESCRIPTION, (p, v) -> p.description = v);
        BINDERS.put(ToolService.PARAMETER_REQUIRED, (p, v) -> {
            if (v != null && !v.isEmpty()) {
                p.required = Boolean.parseBoolean(v);
            }
        });
        BINDERS.put(ToolService.PARAMETER_TYPE, (p, v) -> {
            if (v != null && !v.isEmpty()) {
                if (!SCHEMA_TYPES.contains(v)) {
                    throw new IllegalArgumentException("invalid " + ToolService.PARAMETER_TYPE + " [" + v + "] of tool " + p.name);
                }
                p.type = v;
            }
        });
    }

    @Override
    public Collection<ToolParameter> parse(Map<String, Object> properties) {
        Map<String, ToolParameter> params = new LinkedHashMap<>();
        properties.forEach((key, value) -> {
            int dot = key.indexOf('.');
            if (dot < 0) return;
            BiConsumer<ToolParameter, String> binder = BINDERS.get(key.substring(0, dot));
            if (binder == null) return;
            binder.accept(params.computeIfAbsent(key.substring(dot), k -> new ToolParameter()),
                    Objects.toString(value, null));
        });
        return params.values();
    }
}
