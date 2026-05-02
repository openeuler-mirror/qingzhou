package qingzhou.llm;

public interface Tool {
    String name();

    String description();

    Parameter[] parameters();

    Object invoke(Object... args);

    interface Parameter {
        String name();

        String description();

        Type type();

        boolean required();

        String[] enumValues();
    }

    enum Type {
        STRING, NUMBER, BOOLEAN
    }
}
