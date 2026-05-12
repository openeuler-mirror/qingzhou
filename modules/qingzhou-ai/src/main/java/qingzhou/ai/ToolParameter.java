package qingzhou.ai;

import java.util.List;

public interface ToolParameter {
    String name();

    String description();

    boolean required();

    List<String> enumValues();

    static ToolParameter of(String name, String description,
                        boolean required, List<String> enumValues) {
        return new ToolParameter() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public boolean required() {
                return required;
            }

            @Override
            public List<String> enumValues() {
                return enumValues;
            }
        };
    }

    static ToolParameter of(String name, String description) {
        return of(name, description, true, null);
    }
}
