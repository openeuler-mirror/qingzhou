package qingzhou.framework.util.pattern;

public interface Filter<T> {
    boolean doFilter(T context) throws Exception;

    default void afterFilter(T context) {
    }
}
