package qingzhou.llm;

public interface Tool {
    String name();

    String description();

    ToolParameter[] parameters();

    Object invoke(Object... args);

    static Tool of(String name, String description, ToolParameter[] parameters,
                   java.util.function.Function<Object[], Object> invoke) {
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
            public Object invoke(Object... args) {
                return invoke.apply(args);
            }
        };
    }
}