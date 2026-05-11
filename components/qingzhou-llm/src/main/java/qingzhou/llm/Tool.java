package qingzhou.llm;

import java.util.Map;

public interface Tool {
    String name();

    String description();

    default ToolParameter[] parameters() {
        return null;
    }

    Object invoke(Map<String, Object> argsMap);

    static Tool of(String name, String description, ToolParameter[] parameters,
                   java.util.function.Function<Map<String, Object>, Object> invoke) {
        return new Tool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public ToolParameter[] parameters() {
                return parameters;
            }

            @Override
            public Object invoke(Map<String, Object> argsMap) {
                return invoke.apply(argsMap);
            }
        };
    }
}