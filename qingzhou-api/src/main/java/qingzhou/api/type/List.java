package qingzhou.api.type;

import java.util.Map;

/**
 * 提供列表展示功能的接口，继承自Showable接口。
 */
public interface List {
    String ACTION_LIST = "list";
    String ACTION_ALL = "all";
    String ACTION_CONTAINS = "contains";

    /**
     * 指定 ModelField 指定的字段中，哪一个用作数据 ID
     */
    default String idField() {
        return "id";
    }

    /**
     * 返回此模块所有数据的 ID，返回 null，视作无效，若希望获得以下能力则需要正确实现此方法：
     * 1. 其它模块中有字段通过 ModelField.refModel() 引用了本模块；
     * 2. 需要由轻舟平台在创建本模块数据时候，自动验证是否已经存在
     */
    String[] allIds(Map<String, String> query) throws Exception;

    default boolean contains(String id) throws Exception {
        if (id == null || id.isEmpty()) return false;

        String[] ids = allIds(null);
        if (ids == null) return false;

        for (String s : ids) {
            if (s.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param pageNum    查询此页的数据
     * @param pageSize   每页的数据条数
     * @param showFields 查询出的每条数据的字段名称
     *                   注：当 totalSize() 或 pageSize() 返回值 小于 1 时，请在实现内部忽略分页逻辑，转而返回所有数据
     */
    java.util.List<Map<String, String>> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws Exception;

    /**
     * 如果需要使用列表数据分页查看，则需要覆写此方法，表示所有数据的条数
     * 返回值小于 1 时无效
     */
    default int totalSize(Map<String, String> query) throws Exception {
        String[] ids = allIds(query);
        return ids != null ? ids.length : -1;
    }

    /**
     * 如果需要使用列表数据分页查看，则需要覆写此方法，表示每页的数据条数
     * 返回值小于 1 时无效
     */
    default int pageSize() {
        return 10;
    }

    default String[] headActions() {
        return new String[]{Add.ACTION_CREATE};
    }

    default String[] batchActions() {
        return null;
    }

    default String[] listActions() {
        return new String[]{Update.ACTION_EDIT, Delete.ACTION_DELETE};
    }

    /**
     * 设置进入列表页面默认的搜索条件
     */
    default Map<String, String> defaultSearch() {
        return null;
    }

    default boolean showIdField() {
        return true;
    }

    default boolean showOrderNumber() {
        return true;
    }
}
