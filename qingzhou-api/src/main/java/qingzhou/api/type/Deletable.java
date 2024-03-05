package qingzhou.api.type;

/**
 * Deletable接口定义了删除操作的相关行为，继承自Listable接口。
 *
 */
public interface Deletable extends Listable {
    // 定义删除操作的常量名称
    String ACTION_NAME_DELETE = "delete";
}
