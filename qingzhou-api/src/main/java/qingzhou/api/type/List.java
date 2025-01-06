package qingzhou.api.type;

import java.util.Map;

/**
 * 提供列表展示功能的接口，继承自Showable接口。
 */
public interface List {
    String ACTION_LIST = "list";
    String ACTION_ALL = "all";
    String ACTION_CONTAINS = "contains";
    String ACTION_DEFAULT_SEARCH = "defaultSearch";

    /**
     * @param pageNum    查询此页的数据
     * @param pageSize   每页的数据条数
     * @param showFields 查询出的每条数据的字段名称
     *                   注：当 totalSize() 或 pageSize() 返回值 小于 1
     *                   时，请在实现内部忽略分页逻辑，转而返回所有数据
     */
    java.util.List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query)
            throws Exception;

    /**
     * 如果需要使用列表数据分页查看，则需要覆写此方法，表示所有数据的条数
     * 返回值小于或等于 0 时，列表页面将不显示分页控件
     */
    int totalSize(Map<String, String> query);

    boolean contains(String id);

    /**
     * 单次请求可获取的最大数据条数
     */
    default int maxResponseDataSize() {
        return 500;
    }

    /**
     * 如果需要使用列表数据分页查看，则需要覆写此方法，表示每页的数据条数
     * 返回值小于 1 时无效
     */
    default int pageSize() {
        return 10;
    }

    /**
     * 设置进入列表页面默认的搜索条件
     */
    default Map<String, String> defaultSearch() {
        return null;
    }

    default boolean useDynamicDefaultSearch() {
        return false;
    }

    /**
     * 设置列表页面是否显示数据序号（注：该序号由轻舟生成，为方便用户UI定位，与业务数据无关）
     */
    default boolean showOrderNumber() {
        return true;
    }
}
