package qingzhou.api.type;

import java.util.List;
import java.util.Map;

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

    /**
     * 根据提供的ID数组和字段数组查询列表数据。
     * @param ids 表示查询对象ID的字符串数组。
     * @param fields 需要查询的字段名称的字符串数组。
     * @return 返回一个包含查询结果的Map列表，每个Map代表一条记录，键为字段名，值为字段值。默认返回null。
     */
    default List<Map<String, String>> listData(String[] ids, String[] fields) {
        return null;
    }
}

