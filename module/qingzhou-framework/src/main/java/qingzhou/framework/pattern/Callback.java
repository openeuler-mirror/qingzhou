package qingzhou.framework.pattern;

public interface Callback<T, R> {
    R run(T args) throws Exception;
}
