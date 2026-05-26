package qingzhou.api;

public interface SharedFunction<T, R> {
    R invoke(T t);
}