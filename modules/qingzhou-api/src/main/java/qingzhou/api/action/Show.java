package qingzhou.api.action;

import java.util.Map;

/**
 * 查看单条记录的能力，对应内置动作 {@code show}，同时是编辑表单的数据来源。
 * <p>
 * 返回内容会按 {@code show=true} 的字段裁剪后交给界面；未返回的字段在表单中留空。
 */
public interface Show {
    /**
     * @param id 记录标识，取请求路径中的 id 段
     * @return key 为 {@code @ModelField.code}
     */
    Map<String, String> show(String id) throws Exception;
}
