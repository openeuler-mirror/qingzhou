package qingzhou.api.type;

import java.util.List;
import java.util.Map;

/**
 * 提供列表展示功能的接口，继承自Showable接口。
 */
public interface Listable extends Showable {
    String ACTION_LIST = "list";
    String ACTION_LIST_ALL = "list_all";

    /**
     * 指定列表数据字段中，用作 ID 的字段名
     */
    default String idFieldName() {
        return "id";
    }

    /**
     * 返回此模块所有数据的 ID，非必需，若希望获得以下能力则是必需的：
     * 1. 其它模块中有字段通过 ModelField.refModel() 引用了本模块；
     * 2. 需要由轻舟平台在创建本模块数据时候，自动验证是否已经存在
     */
    default String[] allIds() {
        return null;
    }


    /**
     * @param pageNum    查询此页的数据
     * @param pageSize   每页的数据条数
     * @param fieldNames 查询出的每条数据的字段名称
     *                   注：当 totalSize() 或 pageSize() 返回值 小于 1 时，请在实现内部忽略分页逻辑，转而返回所有数据
     */
    List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) throws Exception;

    /**
     * 如果需要使用列表数据分页查看，则需要覆写此方法，表示所有数据的条数
     * 返回值小于 1 时无效
     */
    default int totalSize() {
        return 0;
    }

    /**
     * 如果需要使用列表数据分页查看，则需要覆写此方法，表示每页的数据条数
     * 返回值小于 1 时无效
     */
    default int pageSize() {
        return 10;
    }
}
