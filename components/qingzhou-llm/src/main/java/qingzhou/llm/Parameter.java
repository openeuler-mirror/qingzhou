package qingzhou.llm;

public interface Parameter {
    String name();

    String description();

    boolean required();

    static Parameter of(String name, String description, boolean required) {
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
        };
    }
}