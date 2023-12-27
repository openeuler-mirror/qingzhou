package qingzhou.framework.pattern;

public interface Visitor<T> {
    /**
     * 访问一个对象，通过返回值反馈是否需要访问下一个
     *
     * @param obj 当前访问的对象
     * @return true 表示继续访问，false 表示终止
     */
    boolean visit(T obj);
}