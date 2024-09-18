package qingzhou.api.type;

import java.util.Map;

/**
 * 提供与编辑和更新操作相关的功能定义。
 */
public interface Updatable extends Showable {
    String ACTION_EDIT = "edit";
    String ACTION_UPDATE = "update";

    void updateData(Map<String, String> data) throws Exception;
}
