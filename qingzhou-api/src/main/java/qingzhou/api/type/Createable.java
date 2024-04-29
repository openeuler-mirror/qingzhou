package qingzhou.api.type;

/**
 * Createable 接口定义了对象的创建能力，扩展了 Deletable 和 Editable 接口。
 * 该接口主要提供了与创建操作相关的功能定义。
 */
public interface Createable extends Deletable, Editable {
    String ACTION_NAME_ADD = "add";
}
