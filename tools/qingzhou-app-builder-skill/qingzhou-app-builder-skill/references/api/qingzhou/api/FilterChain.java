package qingzhou.api;

/**
 * 过滤器链的下一环，由框架传入，索引式推进，同一链实例不保证可重入。
 */
public interface FilterChain {
    /**
     * 继续执行后续过滤器与目标动作；调用返回即表示整条链已处理完毕。
     */
    void doFilter() throws Throwable;
}
