package qingzhou.engine.util;

public interface Visitor<T> {
    // 在循环中如果需要继续访问下一个则返回 true
    boolean visit(T t);
}
