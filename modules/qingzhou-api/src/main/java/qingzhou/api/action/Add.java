package qingzhou.api.action;

import java.util.Map;

/**
 * 新增记录的能力，对应内置动作 {@code add}（表单提交）。
 * <p>
 * 模块实现本接口即自动获得该动作及其按钮、图标、权限元数据，无需再写 {@code @ModelAction}。
 */
public interface Add {
    /**
     * @param data key 为 {@code @ModelField.code}，只包含 {@code add=true} 的表单字段中本次请求传了值的项；
     *             未传字段由应用按需补默认值
     */
    void add(Map<String, String> data) throws Exception;
}
