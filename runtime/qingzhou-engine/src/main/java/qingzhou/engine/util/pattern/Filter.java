package qingzhou.engine.util.pattern;

public interface Filter<T> {
    boolean doFilter(T context) throws Exception;

    default void afterFilter(T context) {
    }
}
