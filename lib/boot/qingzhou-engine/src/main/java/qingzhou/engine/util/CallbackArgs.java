package qingzhou.engine.util;

public interface CallbackArgs<A, R> {
    R callback(A args) throws Throwable;
}
