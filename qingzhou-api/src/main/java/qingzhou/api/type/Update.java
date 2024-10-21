package qingzhou.api.type;

import java.util.Map;
import java.util.Objects;

/**
 * 提供与编辑和更新操作相关的功能定义。
 */
public interface Update {
    String ACTION_EDIT = "edit";
    String ACTION_UPDATE = "update";
    String ACTION_CHANGED = "changed";

    Map<String, String> editData(String id) throws Exception;

    void updateData(Map<String, String> data) throws Exception;

    default boolean changed(String id, String key, String val) throws Exception {
        return !Objects.equals(editData(id).get(key), val);
    }

    default String[] formActions() {
        return null;
    }
}
