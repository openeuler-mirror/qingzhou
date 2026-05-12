package qingzhou.llm;

import java.util.Map;
import java.util.function.Function;

public interface Tool {
    String name();

    String description();

    default Parameter[] parameters() {
        return null;
    }

    Object invoke(Map<String, Object> argsMap) throws Throwable;

    static Tool of(String name, String description, Parameter[] parameters,
                   Function<Map<String, Object>, Object> invoke) {
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