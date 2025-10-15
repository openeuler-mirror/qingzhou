package qingzhou.engine.util.pattern;

public interface CallbackArgs<A, R> {
    R callback(A args) throws Throwable;
}
