package qingzhou.llm;

import java.util.Map;

public interface Tool {
    String name();

    String description();

    default Parameter[] parameters() {
        return null;
    }

    Object invoke(Map<String, Object> argsMap);

    static Tool of(String name, String description, Parameter[] parameters,
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
            public Parameter[] parameters() {
                return parameters;
            }

            @Override
            public Object invoke(Map<String, Object> argsMap) {
                return invoke.apply(argsMap);
            }
        };
    }
}