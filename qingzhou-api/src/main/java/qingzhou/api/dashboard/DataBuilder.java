package qingzhou.api.dashboard;

public interface DataBuilder {
    <T> T build(Class<? extends DataType> dataType);

    void add(DataType dataType);
}
