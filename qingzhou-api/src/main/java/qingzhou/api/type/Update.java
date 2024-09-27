package qingzhou.api.type;

import java.util.Map;

/**
 * 提供与编辑和更新操作相关的功能定义。
 */
public interface Update {
    String ACTION_EDIT = "edit";
    String ACTION_UPDATE = "update";

    Map<String, String> editData(String id) throws Exception;

    void updateData(Map<String, String> data) throws Exception;
}
