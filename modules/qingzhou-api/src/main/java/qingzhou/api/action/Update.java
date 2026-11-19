package qingzhou.api.action;

import java.util.Map;

/**
 * 更新记录的能力，对应内置动作 {@code update}。
 * <p>
 * 支持部分字段提交：只回传本次变更的字段，未传字段由应用保持原值。
 */
public interface Update {
    /**
     * @param id   记录标识，取请求路径中的 id 段
     * @param data key 为 {@code @ModelField.code}，只包含 {@code update=true} 的表单字段中本次传了值的项；
     *             其中 {@code id=true} 的字段已被框架剔除
     */
    void update(String id, Map<String, String> data) throws Exception;
}
