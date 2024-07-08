package qingzhou.api.type;

/**
 * 提供列表展示功能的接口，继承自Showable接口。
 */
public interface Listable extends Showable {
    // 定义列表操作的常量名称
    String ACTION_NAME_LIST = "list";

    // 定义用于表示ID字段的常量名称
    String FIELD_NAME_ID = "id";

    // 定义分页参数页码的常量名称
    String PARAMETER_PAGE_NUM = "pageNum";
}

