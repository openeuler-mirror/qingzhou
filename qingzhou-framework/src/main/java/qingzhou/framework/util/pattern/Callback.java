package qingzhou.framework.util.pattern;

public interface Callback<T, R> {
    R run(T args) throws Exception;
}
