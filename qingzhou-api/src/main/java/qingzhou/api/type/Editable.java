package qingzhou.api.type;

import qingzhou.api.Groups;

import java.util.Map;

/**
 * 提供与编辑和更新操作相关的功能定义。
 */
public interface Editable extends Showable {
    // 页面表单字段分组信息
    default Groups groups() {
        return null;
    }

    void updateData(Map<String, String> data) throws Exception;
}
