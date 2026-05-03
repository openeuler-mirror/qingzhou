package qingzhou.llm;

public interface ToolParameter {
    String name();

    String description();

    ParameterType type();

    boolean required();

    String[] enumValues();

    static ToolParameter of(String name, String description, ParameterType type,
            boolean required, String[] enumValues) {
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
            public ParameterType type() {
                return type;
            }

            @Override
            public boolean required() {
                return required;
            }

            @Override
            public String[] enumValues() {
                return enumValues;
            }
        };
    }

    // 便捷方法：不带枚举值的参数
    static ToolParameter of(String name, String description, ParameterType type, boolean required) {
        return of(name, description, type, required, null);
    }

    // 便捷方法：不带 required 和枚举值的参数
    static ToolParameter of(String name, String description, ParameterType type) {
        return of(name, description, type, false, null);
    }
}