package qingzhou.api.type;

import java.util.List;
import java.util.Map;

/**
 * 提供列表展示功能的接口，继承自Showable接口。
 */
public interface Listable extends Showable {
    String ACTION_LIST = "list";
    String ACTION_ALL = "all";
    String ACTION_CONTAINS = "contains";

    /**
     * 指定 ModelField 指定的字段中，哪一个用作数据 ID
     */
    default String idFieldName() {
        return "id";
    }

    /**
     * 返回此模块所有数据的 ID，返回 null，视作无效，若希望获得以下能力则需要正确实现此方法：
     * 1. 其它模块中有字段通过 ModelField.refModel() 引用了本模块；
     * 2. 需要由轻舟平台在创建本模块数据时候，自动验证是否已经存在
     */
    String[] allIds();

    default boolean contains(String id) {
        String[] ids = allIds();
        if (ids != null) {
            for (String s : ids) {
                if (s.equals(id)) {
                    return true;
                }
            }
            return false;
        }

        return false; // 当不能通过 allIds() 自行判断时，交给应用自行处理
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
        String[] ids = allIds();
        return ids != null ? ids.length : -1;
    }

    /**
     * 如果需要使用列表数据分页查看，则需要覆写此方法，表示每页的数据条数
     * 返回值小于 1 时无效
     */
    default int pageSize() {
        return 10;
    }
}
