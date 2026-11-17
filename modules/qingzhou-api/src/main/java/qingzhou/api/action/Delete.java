package qingzhou.api.action;

/**
 * 删除记录的能力，对应内置动作 {@code delete}。
 * <p>
 * 批量删除由框架解析 {@code ids} 后逐条回调本方法（单次上限 1000），故单条删除必须能独立执行，
 * 不可依赖同一批次的调用顺序。
 */
public interface Delete {
    void delete(String id) throws Exception;
}
