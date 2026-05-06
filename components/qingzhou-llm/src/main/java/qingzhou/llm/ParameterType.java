package qingzhou.llm;

public enum ParameterType {
    STRING("string"), NUMBER("number"), BOOLEAN("boolean");

    final String value;

    ParameterType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
