package qingzhou.api.action;

import java.util.List;

/**
 * 删除记录的能力，对应内置动作 {@code delete}，行内删除与批量删除共用一个动作。
 * <p>
 * 批量删除默认逐条回调 {@link #delete(String)}；数据量大或需要原子性时，应用覆写 {@link #delete(List)}
 * 一次接收整批标识，用单条语句或事务完成，避免 N 次往返与"部分成功"。
 */
public interface Delete {
    /**
     * 删除单条记录，也用于批量删除的默认实现。
     */
    void delete(String id) throws Exception;

    /**
     * 批量删除。默认逐条委托给 {@link #delete(String)}，覆写即由应用负责整批处理。
     * <p>
     * 框架只负责解析 {@code ids} 参数并限制单次数量，不做任何回滚或补偿；需要全有或全无的应用须自行开启事务。
     *
     * @param ids 待删除标识，已去空，单次上限 1000
     */
    default void delete(List<String> ids) throws Exception {
        for (String id : ids) {
            delete(id);
        }
    }
}
