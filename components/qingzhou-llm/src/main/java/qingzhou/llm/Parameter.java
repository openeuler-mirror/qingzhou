package qingzhou.llm;

public interface Parameter {
    String name();

    String description();

    boolean required();

    String[] enumValues();

    static Parameter of(String name, String description,
                        boolean required, String[] enumValues) {
        return new Parameter() {
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
            public String[] enumValues() {
                return enumValues;
            }
        };
    }

    // 便捷方法：不带枚举值的参数
    static Parameter of(String name, String description) {
        return of(name, description, true, null);
    }
}