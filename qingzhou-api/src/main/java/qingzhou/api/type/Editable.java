package qingzhou.api.type;

/**
 * 可编辑接口，继承自可展示接口Showable。
 * 提供与编辑和更新操作相关的功能定义。
 */
public interface Editable extends Showable {
    // 定义编辑操作的常量名称
    String ACTION_NAME_EDIT = "edit";

    // 定义更新操作的常量名称
    String ACTION_NAME_UPDATE = "update";
}

